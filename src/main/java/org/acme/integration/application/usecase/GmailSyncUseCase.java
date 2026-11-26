package org.acme.integration.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exceptions.ConflictException;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.integration.application.GmailAccessRevokedException;
import org.acme.integration.application.IntegrationSettings;
import org.acme.integration.application.model.EmailRule;
import org.acme.integration.application.model.GmailAttachment;
import org.acme.integration.application.model.GmailMessage;
import org.acme.integration.application.model.GmailMessagePage;
import org.acme.integration.application.port.output.DocumentImporter;
import org.acme.integration.application.port.output.EmailRuleRepository;
import org.acme.integration.application.port.output.GmailClient;
import org.acme.integration.application.port.output.IntegrationRepository;
import org.acme.integration.application.port.output.ProcessedItemRepository;
import org.acme.integration.domain.model.Integration;
import org.acme.integration.domain.model.IntegrationProcessedItem;
import org.acme.integration.domain.model.IntegrationProvider;
import org.acme.integration.domain.model.IntegrationStatus;
import org.acme.integration.domain.model.ProcessedItemResult;
import org.acme.provider.domain.model.EmailExtractionMode;
import org.acme.service.application.model.ServiceFilter;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.service.domain.model.CustomerReference;
import org.acme.service.domain.model.Service;
import org.acme.shared.application.model.PageQuery;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trae de Gmail solo los emails de los remitentes de los proveedores (el resto ni se descarga), aplica la regla
 * del proveedor y guarda en Documentos el adjunto o, si la factura viene en el cuerpo, el texto del email. Todo
 * va a la cola de la IA y termina en "Para revisar". Cada mensaje se procesa una sola vez.
 */
@ApplicationScoped
public class GmailSyncUseCase {

    /** Se vuelve a mirar un día antes de la última sincronización por si llegó algo con fecha atrasada. */
    static final Duration OVERLAP = Duration.ofDays(1);

    private static final Logger LOG = Logger.getLogger(GmailSyncUseCase.class);

    // shortcut: el candado vive en memoria; con varias instancias del backend habría que tomarlo en la base
    private final Set<UUID> running = ConcurrentHashMap.newKeySet();

    private final GmailClient gmail;
    private final GmailAccess access;
    private final IntegrationRepository integrations;
    private final ProcessedItemRepository processed;
    private final EmailRuleRepository rules;
    private final ServiceRepository services;
    private final DocumentImporter importer;
    private final IntegrationSettings settings;
    private final Clock clock;

    @Inject
    public GmailSyncUseCase(GmailClient gmail, GmailAccess access, IntegrationRepository integrations, ProcessedItemRepository processed,
                            EmailRuleRepository rules, ServiceRepository services, DocumentImporter importer, IntegrationSettings settings,
                            Clock clock) {
        this.gmail = gmail;
        this.access = access;
        this.integrations = integrations;
        this.processed = processed;
        this.rules = rules;
        this.services = services;
        this.importer = importer;
        this.settings = settings;
        this.clock = clock;
    }

    /** Todas las cuentas activas. Una con problemas no frena a las demás. */
    public void syncAll() {
        for (Integration integration : integrations.findActive(IntegrationProvider.GMAIL)) {
            try {
                sync(integration);
            } catch (RuntimeException e) {
                LOG.errorf("Falló la sincronización de Gmail %s: %s", integration.getId(), e.getClass().getSimpleName());
            }
        }
    }

    /** "Sincronizar ahora". Devuelve cuántos documentos importó. */
    public int syncNow(UUID userId, UUID integrationId) {
        Integration integration = integrations.findByIdForUser(userId, integrationId).orElseThrow(() -> new NotFoundException("integración"));
        if (integration.getStatus() != IntegrationStatus.ACTIVE) {
            throw new ConflictException("Volvé a conectar Gmail para sincronizar");
        }
        return sync(integration);
    }

    /** Si la cuenta ya se está revisando (el botón mientras corre la tarea), no hace nada. */
    int sync(Integration integration) {
        if (!running.add(integration.getId())) {
            return 0;
        }
        try {
            return syncUnlocked(integration);
        } finally {
            running.remove(integration.getId());
        }
    }

    private int syncUnlocked(Integration integration) {
        Instant now = Instant.now(clock);
        String token;
        try {
            token = access.accessToken(integration);
        } catch (GmailAccessRevokedException e) {
            return 0;
        }
        List<EmailRule> active = rules.findActive();
        int imported = 0;
        for (String id : newMessageIds(integration, token, active)) {
            imported += process(integration, token, active, gmail.message(token, id));
        }
        integration.setLastSyncAt(now);
        integration.setLastError(null);
        integrations.save(integration);
        return imported;
    }

    /** Ids de la búsqueda por remitentes que todavía no se procesaron, hasta el máximo por corrida. */
    private List<String> newMessageIds(Integration integration, String token, List<EmailRule> active) {
        Set<String> senders = new LinkedHashSet<>();
        for (EmailRule rule : active) {
            if (rule.senderAddress() != null) senders.add(rule.senderAddress());
            senders.addAll(rule.emailDomains());
        }
        if (senders.isEmpty()) return List.of();
        String query = "from:(" + String.join(" OR ", senders) + ") after:" + since(integration).getEpochSecond();

        List<String> ids = new ArrayList<>();
        String pageToken = null;
        do {
            GmailMessagePage page = gmail.search(token, query, pageToken);
            for (String id : page.ids()) {
                if (ids.size() < settings.maxMessagesPerSync() && !processed.existsMessage(integration.getId(), id)) ids.add(id);
            }
            pageToken = page.nextPageToken();
        } while (pageToken != null && ids.size() < settings.maxMessagesPerSync());
        // shortcut: si hay más que el máximo, los que faltan entran en la próxima corrida solo si siguen dentro de la búsqueda
        return ids;
    }

    private Instant since(Integration integration) {
        if (integration.getLastSyncAt() != null) return integration.getLastSyncAt().minus(OVERLAP);
        if (integration.getSyncFrom() != null) return integration.getSyncFrom().atStartOfDay(ZoneOffset.UTC).toInstant();
        // "Solo los nuevos": desde que se conectó
        return integration.getCreatedAt() != null ? integration.getCreatedAt().minus(OVERLAP) : Instant.now(clock).minus(OVERLAP);
    }

    private int process(Integration integration, String token, List<EmailRule> active, GmailMessage message) {
        Optional<EmailRule> match = EmailRules.match(active, message);
        if (match.isEmpty()) {
            record(integration, message, "", null, null, null, ProcessedItemResult.DISCARDED, "NOT_MATCHED");
            return 0;
        }
        EmailRule rule = match.get();
        UUID userId = integration.getUserId();
        UUID serviceId = service(userId, rule, message);

        List<GmailAttachment> attachments = EmailRules.readableAttachments(rule, message);
        if (attachments.isEmpty()) {
            if (rule.extractionMode() != EmailExtractionMode.EMAIL_BODY) {
                record(integration, message, "", rule, serviceId, null, ProcessedItemResult.DISCARDED, "NO_ATTACHMENT");
                return 0;
            }
            return save(integration, message, "", rule, serviceId,
                    () -> importer.importText(userId, emailText(message), textFilename(message), rule.documentType(), serviceId));
        }
        int imported = 0;
        for (GmailAttachment attachment : attachments) {
            imported += save(integration, message, attachment.attachmentId(), rule, serviceId,
                    () -> importAttachment(userId, token, message, attachment, rule, serviceId));
        }
        return imported;
    }

    private int save(Integration integration, GmailMessage message, String attachmentId, EmailRule rule, UUID serviceId, Import action) {
        try {
            UUID documentId = action.run();
            record(integration, message, attachmentId, rule, serviceId, documentId, ProcessedItemResult.IMPORTED, null);
            return 1;
        } catch (ConflictException e) {
            record(integration, message, attachmentId, rule, serviceId, null, ProcessedItemResult.DUPLICATE, null);
        } catch (RuntimeException e) {
            LOG.errorf("No se pudo importar el email %s: %s", message.id(), e.getClass().getSimpleName());
            record(integration, message, attachmentId, rule, serviceId, null, ProcessedItemResult.FAILED, e.getClass().getSimpleName());
        }
        return 0;
    }

    private UUID importAttachment(UUID userId, String token, GmailMessage message, GmailAttachment attachment, EmailRule rule, UUID serviceId) {
        Path file = null;
        try {
            file = Files.createTempFile("gmail-", ".bin");
            Files.write(file, gmail.attachment(token, message.id(), attachment.attachmentId()));
            return importer.importFile(userId, file, attachment.filename(), rule.documentType(), serviceId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ignored) {
                    // Es un temporal: si no se pudo borrar lo limpia el sistema
                }
            }
        }
    }

    /** El servicio por el nro. de cliente del email; si no, el único servicio activo del proveedor; si no, null. */
    private UUID service(UUID userId, EmailRule rule, GmailMessage message) {
        List<Service> ofProvider = services.findForUser(userId, new ServiceFilter(null, null, true), new PageQuery(0, PageQuery.MAX_SIZE)).content()
                .stream().filter(s -> rule.providerId().equals(s.getProviderId())).toList();
        Optional<String> reference = EmailRules.customerReference(rule, message).map(CustomerReference::normalize);
        if (reference.isPresent()) {
            Optional<Service> byReference = ofProvider.stream()
                    .filter(s -> s.getCustomerReference() != null && reference.get().equals(CustomerReference.normalize(s.getCustomerReference())))
                    .findFirst();
            if (byReference.isPresent()) return byReference.get().getId();
        }
        return ofProvider.size() == 1 ? ofProvider.getFirst().getId() : null;
    }

    private void record(Integration integration, GmailMessage message, String attachmentId, EmailRule rule, UUID serviceId, UUID documentId,
                        ProcessedItemResult result, String reason) {
        processed.save(IntegrationProcessedItem.builder()
                .integrationId(integration.getId())
                .externalMessageId(message.id())
                .externalAttachmentId(attachmentId)
                .providerId(rule == null ? null : rule.providerId())
                .ruleId(rule == null ? null : rule.ruleId())
                .serviceId(serviceId)
                .documentId(documentId)
                .sender(message.sender())
                .subject(truncate(message.subject(), 500))
                .receivedAt(message.receivedAt())
                .result(result)
                .discardReason(reason)
                .processedAt(Instant.now(clock))
                .build());
    }

    /** El email como texto para la IA: de quién, asunto, fecha y cuerpo. */
    static String emailText(GmailMessage message) {
        return "De: " + message.sender() + "\nAsunto: " + message.subject() + "\nFecha: " + message.receivedAt() + "\n\n" + message.bodyText();
    }

    static String textFilename(GmailMessage message) {
        String subject = message.subject() == null || message.subject().isBlank() ? "Email" : message.subject();
        return truncate(subject.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ").trim(), 120) + ".txt";
    }

    private static String truncate(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }
}
