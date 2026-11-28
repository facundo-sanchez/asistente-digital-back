package org.acme.invoice.application.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.acme.document.application.usecase.ExtractionReviews;
import org.acme.document.application.usecase.InMemoryDocumentRepository;
import org.acme.document.application.usecase.InMemoryExtractionResultRepository;
import org.acme.document.application.usecase.InMemoryProcessingJobRepository;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.Document;
import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.domain.exceptions.ServiceException;
import org.acme.invoice.application.model.ExtractedInvoice;
import org.acme.invoice.application.model.InvoiceConfirmation;
import org.acme.invoice.application.model.InvoiceReview;
import org.acme.invoice.domain.model.InvoiceSource;
import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.service.application.usecase.InMemoryServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.shared.domain.model.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Revisar y confirmar una factura leída por la IA. */
class InvoiceExtractionUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final ExtractedInvoice GAS = new ExtractedInvoice("Camuzzi Gas Pampeana", "10-123.456/7", "0001-00001234", "2026-11",
            LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 5), new BigDecimal("48500.50"), "ARS", "1234 5678");

    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private InMemoryExtractionResultRepository extractions;
    private InMemoryInvoiceRepository invoices;
    private InMemoryServiceRepository services;
    private InMemoryDocumentRepository documents;
    private InvoiceExtractionUseCase useCase;
    private UUID gas;
    private UUID coopelectric;
    private final org.acme.provider.application.usecase.InMemoryProviderRepository providers = new org.acme.provider.application.usecase.InMemoryProviderRepository();

    @BeforeEach
    void setUp() {
        extractions = new InMemoryExtractionResultRepository();
        invoices = new InMemoryInvoiceRepository();
        services = new InMemoryServiceRepository();
        documents = new InMemoryDocumentRepository();
        gas = services.save(Service.builder().userId(ANA).providerId(UUID.randomUUID()).name("Gas casa").customerReference("101234567").build()).getId();
        services.save(Service.builder().userId(ANA).providerId(UUID.randomUUID()).name("Luz casa").build());
        coopelectric = providers.add(org.acme.provider.domain.model.Provider.builder().name("Coopelectric").code("COOPELECTRIC")
                .category(org.acme.shared.domain.model.ServiceCategory.WATER).build()).getId();
        Clock clock = Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC);
        var register = new RegisterInvoiceUseCase(invoices, new InMemoryObligationRepository(),
                new InvoiceDraftValidator(services, documents, invoices), new PriceIncreaseCheck(invoices, new org.acme.user.application.usecase.InMemoryUserRepository(Clock.systemUTC()), (i, n, o, p, a2) -> {}), clock);
        useCase = new InvoiceExtractionUseCase(new ExtractionReviews(extractions, documents, new InMemoryProcessingJobRepository(), clock),
                services, invoices, register, json, documentId -> java.util.Optional.empty(),
                new org.acme.provider.application.usecase.ProviderMatcher(new org.acme.provider.application.usecase.ProviderQueryUseCase(providers)));
    }

    @Test
    void subidaAManoSinServicioReconoceElProveedorParaRegistrarlo() {
        InvoiceReview review = useCase.review(ANA, extraction(new ExtractedInvoice("Coopelectric Ltda.", "73812", null, "2025-12",
                null, LocalDate.of(2026, 2, 16), new BigDecimal("9042.56"), "ARS", null)));

        assertNull(review.suggestedServiceId());
        assertEquals(coopelectric, review.suggestedProviderId());
    }

    @Test
    void sugiereElServicioPorNumeroDeSocioAunqueEsteEscritoDistinto() {
        InvoiceReview review = useCase.review(ANA, extraction(GAS));

        assertEquals(gas, review.suggestedServiceId());
        assertTrue(review.warnings().isEmpty());
    }

    @Test
    void confirmarCreaLaFacturaConSuVencimientoYNoSePuedeConfirmarDosVeces() {
        UUID id = extraction(GAS);

        var view = useCase.confirm(ANA, id, confirmation(GAS.dueDate()));

        assertEquals(InvoiceSource.AI_EXTRACTION, view.invoice().getSource());
        assertEquals(id, view.invoice().getExtractionResultId());
        assertNotNull(view.obligation());
        assertEquals(ExtractionValidationStatus.CONFIRMED, extractions.rows.get(id).getValidationStatus());
        assertEquals(view.invoice().getId(), useCase.review(ANA, id).invoiceId());
        assertThrows(ServiceException.class, () -> useCase.confirm(ANA, id, confirmation(GAS.dueDate())));
    }

    @Test
    void sinVencimientoNoSeConfirmaYCompletarloQuedaComoCorreccion() {
        UUID id = extraction(new ExtractedInvoice("Camuzzi", null, null, "2026-11", null, null, new BigDecimal("100"), "ARS", null));

        assertTrue(useCase.review(ANA, id).warnings().stream().anyMatch(w -> w.contains("vencimiento")));
        assertThrows(ServiceException.class, () -> useCase.confirm(ANA, id, new InvoiceConfirmation(gas, null, "2026-11", null, null, new BigDecimal("100"), "ARS", null)));
        useCase.confirm(ANA, id, new InvoiceConfirmation(gas, null, "2026-11", null, LocalDate.of(2026, 12, 5), new BigDecimal("100"), "ARS", null));

        assertEquals(ExtractionValidationStatus.CORRECTED, extractions.rows.get(id).getValidationStatus());
    }

    private InvoiceConfirmation confirmation(LocalDate dueDate) {
        return new InvoiceConfirmation(gas, GAS.invoiceNumber(), GAS.billingPeriod(), GAS.issueDate(), dueDate, GAS.totalAmount(), "ARS", GAS.paymentCode());
    }

    private UUID extraction(ExtractedInvoice invoice) {
        UUID document = documents.save(Document.builder().userId(ANA).documentType(DocumentType.INVOICE).originalFilename("gas.pdf")
                .checksum(UUID.randomUUID().toString()).storageKey("k").build()).getId();
        Map<String, Object> data = json.convertValue(invoice, new TypeReference<>() {
        });
        return extractions.save(AiExtractionResult.builder().documentId(document).userId(ANA).model("test")
                .schemaVersion(ExtractedInvoice.SCHEMA_VERSION).detectedType(DocumentType.INVOICE).extractedData(data).build()).getId();
    }
}
