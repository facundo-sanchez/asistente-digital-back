package org.acme.invoice.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.document.application.usecase.ExtractionReviews;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.domain.exceptions.ServiceException;
import org.acme.invoice.application.model.ExtractedInvoice;
import org.acme.invoice.application.model.InvoiceConfirmation;
import org.acme.invoice.application.model.InvoiceReview;
import org.acme.invoice.application.model.InvoiceView;
import org.acme.invoice.application.port.output.InvoiceRepository;
import org.acme.invoice.domain.model.Invoice;
import org.acme.invoice.domain.model.InvoiceSource;
import org.acme.service.application.model.ServiceFilter;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.service.domain.model.CustomerReference;
import org.acme.service.domain.model.Service;
import org.acme.shared.application.model.PageQuery;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Revisar y confirmar una factura leída por la IA (CU-002, CU-004). Confirmar es lo mismo que
 * cargarla a mano: queda confirmada y genera su vencimiento.
 */
@ApplicationScoped
public class InvoiceExtractionUseCase {

    static final String SERVICE_REQUIRED = "Elegí el servicio de la factura";
    static final String DUE_DATE_REQUIRED = "Completá la fecha de vencimiento";
    static final String AMOUNT_REQUIRED = "Completá el importe";

    private final ExtractionReviews reviews;
    private final ServiceRepository services;
    private final InvoiceRepository invoices;
    private final RegisterInvoiceUseCase registerInvoice;
    private final ObjectMapper json;

    @Inject
    public InvoiceExtractionUseCase(ExtractionReviews reviews, ServiceRepository services, InvoiceRepository invoices,
                                    RegisterInvoiceUseCase registerInvoice, ObjectMapper json) {
        this.reviews = reviews;
        this.services = services;
        this.invoices = invoices;
        this.registerInvoice = registerInvoice;
        this.json = json;
    }

    public InvoiceReview review(UUID userId, UUID extractionId) {
        AiExtractionResult result = reviews.find(userId, extractionId, ExtractedInvoice.SCHEMA_VERSION);
        ExtractedInvoice invoice = parse(result);
        Optional<Service> service = suggestService(userId, invoice);
        List<String> warnings = new ArrayList<>();
        if (service.isEmpty()) {
            warnings.add("No encontramos el servicio de esta factura: elegilo o registralo.");
        }
        if (invoice.dueDate() == null) {
            warnings.add("No se leyó el vencimiento: completalo antes de confirmar.");
        }
        if (invoice.totalAmount() == null) {
            warnings.add("No se leyó el importe: completalo antes de confirmar.");
        }
        if (service.isPresent() && result.getValidationStatus() == ExtractionValidationStatus.PENDING
                && !invoices.findPossibleDuplicates(userId, service.get().getId(), invoice.invoiceNumber(), invoice.billingPeriod(), invoice.totalAmount()).isEmpty()) {
            warnings.add("Puede que ya hayas cargado esta factura (mismo número, o mismo período e importe).");
        }
        UUID invoiceId = invoices.findByExtraction(userId, extractionId).map(Invoice::getId).orElse(null);
        return new InvoiceReview(result.getId(), result.getDocumentId(), result.getValidationStatus(), invoice,
                service.map(Service::getId).orElse(null), warnings, invoiceId);
    }

    @Transactional
    public InvoiceView confirm(UUID userId, UUID extractionId, InvoiceConfirmation confirmation) {
        AiExtractionResult result = reviews.pending(userId, extractionId, ExtractedInvoice.SCHEMA_VERSION);
        if (confirmation.serviceId() == null) {
            throw new ServiceException(SERVICE_REQUIRED);
        }
        if (confirmation.dueDate() == null) {
            throw new ServiceException(DUE_DATE_REQUIRED);
        }
        if (confirmation.totalAmount() == null) {
            throw new ServiceException(AMOUNT_REQUIRED);
        }
        // Mismas validaciones y vencimiento que la carga manual (servicio del usuario, RN-005)
        InvoiceView view = registerInvoice.register(userId, Invoice.builder()
                .serviceId(confirmation.serviceId())
                .documentId(result.getDocumentId())
                .extractionResultId(result.getId())
                .source(InvoiceSource.AI_EXTRACTION)
                .invoiceNumber(confirmation.invoiceNumber())
                .billingPeriod(confirmation.billingPeriod())
                .issueDate(confirmation.issueDate())
                .dueDate(confirmation.dueDate())
                .totalAmount(confirmation.totalAmount())
                .currency(confirmation.currency() == null ? "ARS" : confirmation.currency())
                .paymentCode(confirmation.paymentCode())
                .build());
        ExtractedInvoice extracted = parse(result);
        reviews.close(userId, result, changed(extracted, confirmation) ? ExtractionValidationStatus.CORRECTED : ExtractionValidationStatus.CONFIRMED,
                confirmation.serviceId());
        return view;
    }

    /** Descartar la propuesta: no se crea nada; el documento queda guardado. */
    @Transactional
    public void reject(UUID userId, UUID extractionId) {
        reviews.close(userId, reviews.pending(userId, extractionId, ExtractedInvoice.SCHEMA_VERSION), ExtractionValidationStatus.REJECTED, null);
    }

    /**
     * El servicio del usuario con ese número de socio (es lo que asocia facturas y servicios); si no
     * hay, el único cuyo nombre incluye el del proveedor.
     */
    // shortcut: mira los primeros 100 servicios del usuario; con más, buscar en la base
    Optional<Service> suggestService(UUID userId, ExtractedInvoice invoice) {
        List<Service> all = services.findForUser(userId, new ServiceFilter(null, null, null), new PageQuery(0, PageQuery.MAX_SIZE)).content();
        String reference = invoice.customerReference() == null ? null : CustomerReference.normalize(invoice.customerReference());
        if (reference != null) {
            Optional<Service> byReference = all.stream()
                    .filter(s -> s.getCustomerReference() != null && reference.equals(CustomerReference.normalize(s.getCustomerReference())))
                    .findFirst();
            if (byReference.isPresent()) {
                return byReference;
            }
        }
        if (invoice.provider() == null || invoice.provider().isBlank()) {
            return Optional.empty();
        }
        String word = invoice.provider().trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
        List<Service> byName = all.stream().filter(s -> s.getName().toLowerCase(Locale.ROOT).contains(word)).toList();
        return byName.size() == 1 ? Optional.of(byName.getFirst()) : Optional.empty();
    }

    private static boolean changed(ExtractedInvoice e, InvoiceConfirmation c) {
        return !Objects.equals(e.invoiceNumber(), c.invoiceNumber()) || !Objects.equals(e.billingPeriod(), c.billingPeriod())
                || !Objects.equals(e.issueDate(), c.issueDate()) || !Objects.equals(e.dueDate(), c.dueDate())
                || !Objects.equals(e.paymentCode(), c.paymentCode())
                || e.totalAmount() == null || e.totalAmount().compareTo(Objects.requireNonNullElse(c.totalAmount(), BigDecimal.ZERO)) != 0;
    }

    private ExtractedInvoice parse(AiExtractionResult result) {
        return json.convertValue(result.getExtractedData(), ExtractedInvoice.class);
    }
}
