package org.acme.invoice.application.usecase;

import org.acme.document.application.usecase.InMemoryDocumentRepository;
import org.acme.document.domain.model.Document;
import org.acme.domain.exceptions.ConflictException;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.invoice.application.model.InvoiceFilter;
import org.acme.invoice.application.model.InvoiceView;
import org.acme.invoice.domain.model.Invoice;
import org.acme.invoice.domain.model.InvoiceStatus;
import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.obligation.domain.model.ObligationType;
import org.acme.service.application.usecase.InMemoryServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.shared.application.model.PageQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Carga manual de facturas (CU-002): validaciones, vencimiento generado y consulta. */
class InvoiceUseCasesTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-25T12:00:00Z");
    private static final PageQuery FIRST_PAGE = new PageQuery(0, 20);

    private InMemoryInvoiceRepository invoices;
    private InMemoryObligationRepository obligations;
    private InMemoryDocumentRepository documents;
    private RegisterInvoiceUseCase register;
    private InvoiceQueryUseCase query;
    private ManageInvoiceUseCase manage;
    private UUID light;
    private UUID betoGas;

    @BeforeEach
    void setUp() {
        invoices = new InMemoryInvoiceRepository();
        obligations = new InMemoryObligationRepository();
        documents = new InMemoryDocumentRepository();
        InMemoryServiceRepository services = new InMemoryServiceRepository();
        light = services.save(Service.builder().userId(ANA).providerId(UUID.randomUUID()).name("Luz casa").build()).getId();
        betoGas = services.save(Service.builder().userId(BETO).providerId(UUID.randomUUID()).name("Gas Beto").build()).getId();
        InvoiceDraftValidator validator = new InvoiceDraftValidator(services, documents, invoices);
        register = new RegisterInvoiceUseCase(invoices, obligations, validator, Clock.fixed(NOW, ZoneOffset.UTC));
        query = new InvoiceQueryUseCase(invoices, services, obligations);
        manage = new ManageInvoiceUseCase(invoices, obligations, validator, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void quedaConfirmadaYGeneraSuVencimiento() {
        InvoiceView view = register.register(ANA, draft(light, " 0001-00001234 ", "2026-12"));

        Invoice invoice = view.invoice();
        assertEquals(InvoiceStatus.CONFIRMED, invoice.getStatus());
        assertEquals(NOW, invoice.getConfirmedAt());
        assertEquals("0001-00001234", invoice.getInvoiceNumber());
        assertEquals("Luz casa", view.serviceName());

        Obligation obligation = view.obligation().obligation();
        assertEquals("Luz casa · factura 2026-12", obligation.getTitle());
        assertEquals(invoice.getId(), obligation.getInvoiceId());
        assertEquals(light, obligation.getServiceId());
        assertEquals(ObligationType.INVOICE_PAYMENT, obligation.getObligationType());
        assertEquals(new BigDecimal("48500"), obligation.getAmount());
        assertEquals(LocalDate.of(2026, 12, 15), obligation.getDueDate());
    }

    @Test
    void sinPeriodoElVencimientoUsaElNumeroYSinNingunoQuedaGenerico() {
        assertEquals("Luz casa · factura A-77", register.register(ANA, draft(light, "A-77", " ")).obligation().obligation().getTitle());
        assertEquals("Luz casa · factura", register.register(ANA, draft(light, null, null)).obligation().obligation().getTitle());
    }

    @Test
    void rechazaServicioODocumentoAjenoMonedaYFechas() {
        assertThrows(ServiceException.class, () -> register.register(ANA, draft(betoGas, null, null)));
        UUID betoDoc = documents.save(Document.builder().userId(BETO).originalFilename("f.pdf").checksum("c").storageKey("k").build()).getId();
        assertThrows(ServiceException.class, () -> register.register(ANA, draft(light, null, null).toBuilder().documentId(betoDoc).build()));
        assertThrows(ServiceException.class, () -> register.register(ANA, draft(light, null, null).toBuilder().currency("EUR").build()));
        assertThrows(ServiceException.class, () -> register.register(ANA, draft(light, null, null).toBuilder().issueDate(LocalDate.of(2026, 12, 20)).build()));
        assertTrue(invoices.rows.isEmpty());
        assertTrue(obligations.rows.isEmpty());
    }

    @Test
    void elMismoNumeroDelMismoServicioNoSeCargaDosVeces() {
        register.register(ANA, draft(light, "0001-1", "2026-11"));

        assertThrows(ConflictException.class, () -> register.register(ANA, draft(light, "0001-1", "2026-12")));
        assertEquals(1, invoices.rows.size());
    }

    @Test
    void listaConElEstadoDeSuVencimientoYSoloLasDelUsuario() {
        register.register(ANA, draft(light, "0001-1", "2026-11").toBuilder().issueDate(LocalDate.of(2026, 11, 1)).dueDate(LocalDate.of(2026, 11, 15)).build());
        register.register(ANA, draft(light, "0001-2", "2026-12"));

        List<InvoiceView> page = query.list(ANA, new InvoiceFilter(null, null, null, null, null), FIRST_PAGE).content();
        assertEquals(List.of("0001-2", "0001-1"), page.stream().map(v -> v.invoice().getInvoiceNumber()).toList());
        assertEquals(EffectiveObligationStatus.PENDING, page.getFirst().obligation().effectiveStatus());
        assertEquals("Luz casa", page.getFirst().serviceName());
        assertEquals(1, query.list(ANA, new InvoiceFilter("-2", null, null, null, null), FIRST_PAGE).totalElements());
        assertEquals(0, query.list(BETO, new InvoiceFilter(null, null, null, null, null), FIRST_PAGE).totalElements());

        UUID id = page.getFirst().invoice().getId();
        assertThrows(NotFoundException.class, () -> query.get(BETO, id));
        assertNotNull(query.get(ANA, id).obligation());
    }

    @Test
    void avisaRepetidasPorNumeroOPorPeriodoEImporte() {
        UUID id = register.register(ANA, draft(light, "0001-1", "2026-12")).invoice().getId();

        assertEquals(1, query.duplicates(ANA, light, "0001-1", null, null, null).size());
        assertEquals(1, query.duplicates(ANA, light, null, "2026-12", new BigDecimal("48500.00"), null).size());
        assertEquals(0, query.duplicates(ANA, light, null, "2026-12", new BigDecimal("1"), null).size());
        assertEquals(0, query.duplicates(ANA, light, "0001-1", null, null, id).size(), "al editar no cuenta la propia");
        assertEquals(0, query.duplicates(BETO, light, "0001-1", null, null, null).size(), "servicio ajeno");
    }

    @Test
    void corregirCopiaLosDatosAlVencimiento() {
        InvoiceView created = register.register(ANA, draft(light, "0001-1", "2026-12"));

        InvoiceView fixed = manage.update(ANA, created.invoice().getId(),
                draft(light, "0001-1", "2027-01").toBuilder().totalAmount(new BigDecimal("50000")).dueDate(LocalDate.of(2027, 1, 10)).build());

        Obligation obligation = obligations.rows.get(fixed.obligation().obligation().getId());
        assertEquals("Luz casa · factura 2027-01", obligation.getTitle());
        assertEquals(new BigDecimal("50000"), obligation.getAmount());
        assertEquals(LocalDate.of(2027, 1, 10), obligation.getDueDate());
    }

    @Test
    void anularCancelaElVencimientoPeroNoSiTienePagos() {
        InvoiceView paid = register.register(ANA, draft(light, "0001-1", "2026-11"));
        InvoiceView free = register.register(ANA, draft(light, "0001-2", "2026-12"));
        obligations.paid.put(paid.obligation().obligation().getId(), new BigDecimal("100"));

        assertThrows(ServiceException.class, () -> manage.reject(ANA, paid.invoice().getId()));
        assertThrows(ServiceException.class, () -> manage.update(ANA, paid.invoice().getId(), draft(light, "0001-1", "2026-11")));
        manage.reject(ANA, free.invoice().getId());

        assertEquals(InvoiceStatus.REJECTED, invoices.rows.get(free.invoice().getId()).getStatus());
        assertEquals(ObligationStatus.CANCELLED, obligations.rows.get(free.obligation().obligation().getId()).getStatus());
        assertThrows(ServiceException.class, () -> manage.reject(ANA, free.invoice().getId()), "ya anulada");
        assertThrows(NotFoundException.class, () -> manage.reject(BETO, paid.invoice().getId()));
    }

    private static Invoice draft(UUID serviceId, String number, String period) {
        return Invoice.builder()
                .serviceId(serviceId).invoiceNumber(number).billingPeriod(period)
                .issueDate(LocalDate.of(2026, 12, 1)).dueDate(LocalDate.of(2026, 12, 15))
                .totalAmount(new BigDecimal("48500")).currency("ARS")
                .build();
    }
}
