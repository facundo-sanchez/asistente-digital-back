package org.acme.integration.application.usecase;

import org.acme.integration.application.model.EmailRule;
import org.acme.integration.application.model.GmailAttachment;
import org.acme.integration.application.model.GmailMessage;
import org.acme.integration.domain.model.Integration;
import org.acme.integration.domain.model.IntegrationProcessedItem;
import org.acme.integration.domain.model.IntegrationStatus;
import org.acme.integration.domain.model.ProcessedItemResult;
import org.acme.provider.domain.model.EmailExtractionMode;
import org.acme.service.application.usecase.InMemoryServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.shared.domain.model.DocumentType;
import org.acme.user.application.usecase.TestAppSettings;
import org.acme.user.application.usecase.TestGoogleSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Conectar Gmail y sincronizar: solo los remitentes de los proveedores, sin repetir, con el servicio asociado. */
class GmailIntegrationTest {

    private static final UUID CAMUZZI = UUID.randomUUID();
    private static final UUID BANCO = UUID.randomUUID();
    private static final List<EmailRule> RULES = List.of(
            new EmailRule(UUID.randomUUID(), CAMUZZI, "factura@factura.camuzzigas.com.ar", List.of("camuzzigas.com.ar"),
                    "(?i)camuzzi te acerca tu factura", null, DocumentType.INVOICE, EmailExtractionMode.EMAIL_BODY,
                    "(?i)Nro\\.?\\s*Cuenta:?\\s*([0-9][0-9/-]*[0-9])"),
            new EmailRule(UUID.randomUUID(), BANCO, null, List.of("banco.com.ar"), null, "(?i)resumen.*\\.pdf", DocumentType.CARD_STATEMENT,
                    EmailExtractionMode.ATTACHMENT, null));

    private final FakeGmailClient gmail = new FakeGmailClient();
    private final InMemoryIntegrationRepository integrations = new InMemoryIntegrationRepository();
    private final InMemoryCredentialRepository credentials = new InMemoryCredentialRepository();
    private final InMemoryProcessedItemRepository processed = new InMemoryProcessedItemRepository();
    private final InMemoryServiceRepository services = new InMemoryServiceRepository();
    private final RecordingDocumentImporter importer = new RecordingDocumentImporter();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-26T12:30:00Z"), ZoneOffset.UTC);
    private final UUID ana = UUID.randomUUID();
    private final List<Integration> alerts = new java.util.ArrayList<>();

    private ConnectGmailUseCase connect;
    private GmailSyncUseCase sync;

    @BeforeEach
    void setUp() {
        PlainTokenCipher cipher = new PlainTokenCipher();
        GmailAccess access = new GmailAccess(gmail, integrations, credentials, cipher, alerts::add, clock);
        connect = new ConnectGmailUseCase(gmail, integrations, credentials, processed, cipher, new TestAppSettings(new TestGoogleSettings(true)),
                new TestIntegrationSettings(), clock);
        sync = new GmailSyncUseCase(gmail, access, integrations, processed, userId -> RULES, services, importer, new TestIntegrationSettings(),
                (userId, providerId, notice) -> null, new org.acme.user.application.usecase.InMemoryUserRepository(clock), clock);
    }

    @Test
    void importaElCuerpoOElAdjuntoYDescartaElResto() {
        UUID gas = services.save(Service.builder().userId(ana).providerId(CAMUZZI).name("Gas casa").customerReference("10-123.456/7").build()).getId();
        services.save(Service.builder().userId(ana).providerId(CAMUZZI).name("Gas quinta").customerReference("99").build());
        Integration gmailAccount = connect.connect(ana, "c1", "v1", null);

        gmail.inbox.put("m1", message("m1", "factura@factura.camuzzigas.com.ar", "Camuzzi te acerca tu factura", "Nro. Cuenta: 10123456/7 Total: $ 45.000,00"));
        gmail.inbox.put("m2", new GmailMessage("m2", "avisos@mail.banco.com.ar", "Tu resumen", Instant.now(clock), "",
                List.of(new GmailAttachment("a1", "Resumen-Visa.pdf", "application/pdf"), new GmailAttachment("a2", "logo.png", "image/png"))));
        gmail.inbox.put("m3", message("m3", "otro@camuzzigas.com.ar.falso.com", "Camuzzi te acerca tu factura", "phishing"));

        assertEquals(2, sync.syncNow(ana, gmailAccount.getId()));

        assertEquals(List.of("Camuzzi te acerca tu factura.txt (INVOICE)", "Resumen-Visa.pdf (CARD_STATEMENT)"), importer.imported);
        assertEquals(gas, importer.services.getFirst(), "asociada por el nro. de cuenta");
        assertEquals(ProcessedItemResult.DISCARDED, result("m3"), "un dominio parecido no es del proveedor");
        assertTrue(gmail.queries.getFirst().startsWith("from:(factura@factura.camuzzigas.com.ar OR camuzzigas.com.ar OR banco.com.ar) after:"));

        assertEquals(0, sync.syncNow(ana, gmailAccount.getId()), "lo procesado no se vuelve a procesar");
        assertEquals(3, connect.detected(ana, gmailAccount.getId(), new org.acme.shared.application.model.PageQuery(0, 20)).totalElements());
        assertThrows(RuntimeException.class, () -> connect.detected(UUID.randomUUID(), gmailAccount.getId(), new org.acme.shared.application.model.PageQuery(0, 20)));
        assertNotNull(integrations.rows.get(gmailAccount.getId()).getLastSyncAt());
    }

    @Test
    void elMismoArchivoEnOtroEmailEsDuplicado() {
        Integration account = connect.connect(ana, "c1", "v1", null);
        gmail.inbox.put("m1", message("m1", "factura@factura.camuzzigas.com.ar", "Camuzzi te acerca tu factura", "igual"));
        sync.syncNow(ana, account.getId());
        gmail.inbox.clear();
        // Otro email con el mismo remitente, asunto, fecha y texto: mismo documento
        gmail.inbox.put("m9", message("m9", "factura@factura.camuzzigas.com.ar", "Camuzzi te acerca tu factura", "igual"));

        sync.syncNow(ana, account.getId());

        assertEquals(1, importer.imported.size());
    }

    @Test
    void loQueFallaSeReintentaEnLaProximaRevision() {
        Integration account = connect.connect(ana, "c1", "v1", null);
        gmail.inbox.put("m1", new GmailMessage("m1", "factura@factura.camuzzigas.com.ar", "Camuzzi te acerca tu factura",
                Instant.parse("2026-09-01T10:00:00Z"), "Total: $ 1,00", List.of()));
        importer.failNext = true;

        assertEquals(0, sync.syncNow(ana, account.getId()));
        assertEquals(ProcessedItemResult.FAILED, result("m1"));

        int queries = gmail.queries.size();
        assertEquals(1, sync.syncNow(ana, account.getId()));
        assertEquals(ProcessedItemResult.IMPORTED, result("m1"));
        long after = Long.parseLong(gmail.queries.get(queries).replaceAll(".*after:", ""));
        assertTrue(after <= Instant.parse("2026-09-01T10:00:00Z").getEpochSecond(), "busca desde el email que falló, no desde la última revisión");
        assertEquals(1, processed.rows.size(), "el reintento reemplaza la fila que falló");
    }

    @Test
    void siGoogleRevocaElPermisoPideReconectar() {
        Integration account = connect.connect(ana, "c1", "v1", null);
        gmail.refreshRevoked = true;
        Clock later = Clock.fixed(Instant.parse("2026-11-26T15:00:00Z"), ZoneOffset.UTC);
        GmailSyncUseCase laterSync = new GmailSyncUseCase(gmail, new GmailAccess(gmail, integrations, credentials, new PlainTokenCipher(), alerts::add, later),
                integrations, processed, userId -> RULES, services, importer, new TestIntegrationSettings(),
                (userId, providerId, notice) -> null, new org.acme.user.application.usecase.InMemoryUserRepository(later), later);

        laterSync.syncAll();

        assertEquals(IntegrationStatus.REAUTH_REQUIRED, integrations.rows.get(account.getId()).getStatus());
        assertEquals(1, alerts.size(), "se avisa una vez para reconectar");
        laterSync.syncAll();
        assertTrue(gmail.queries.isEmpty(), "una cuenta que pide reconectar no se sincroniza");
    }

    @Test
    void desconectarRevocaYBorraLosTokensPeroNoLoImportado() {
        Integration account = connect.connect(ana, "c1", "v1", null);
        assertEquals("ana@gmail.com", account.getExternalAccountId());

        connect.disconnect(ana, account.getId());

        assertEquals(List.of("refresh-c1"), gmail.revoked);
        assertTrue(credentials.rows.isEmpty());
        assertEquals(IntegrationStatus.DISCONNECTED, integrations.rows.get(account.getId()).getStatus());
        assertTrue(connect.list(ana).isEmpty());
        assertThrows(RuntimeException.class, () -> connect.disconnect(UUID.randomUUID(), account.getId()), "otro usuario no la ve");
    }

    private GmailMessage message(String id, String sender, String subject, String body) {
        return new GmailMessage(id, sender, subject, Instant.parse("2026-11-26T10:00:00Z"), body, List.of());
    }

    private ProcessedItemResult result(String messageId) {
        return processed.rows.stream().filter(i -> i.getExternalMessageId().equals(messageId)).map(IntegrationProcessedItem::getResult)
                .findFirst().orElseThrow();
    }
}
