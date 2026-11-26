package org.acme.invoice.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.domain.exceptions.ConflictException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.invoice.application.port.output.InvoiceRepository;
import org.acme.invoice.domain.model.Invoice;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.service.domain.model.Service;

import java.util.Set;
import java.util.UUID;

/** Reglas comunes al alta y a la edición de una factura. */
@ApplicationScoped
public class InvoiceDraftValidator {

    static final String UNKNOWN_SERVICE = "El servicio elegido no existe";
    static final String UNKNOWN_DOCUMENT = "El documento elegido no existe";
    static final String INVALID_CURRENCY = "La moneda tiene que ser ARS o USD";
    static final String INVALID_DATES = "El vencimiento no puede ser anterior a la fecha de emisión";
    static final String DUPLICATE_NUMBER = "Ya cargaste la factura %s de este servicio";

    private static final Set<String> CURRENCIES = Set.of("ARS", "USD");

    private final ServiceRepository services;
    private final DocumentRepository documents;
    private final InvoiceRepository invoices;

    @Inject
    public InvoiceDraftValidator(ServiceRepository services, DocumentRepository documents, InvoiceRepository invoices) {
        this.services = services;
        this.documents = documents;
        this.invoices = invoices;
    }

    /**
     * Valida el borrador y devuelve el nombre de su servicio. keepId: al editar, la propia
     * factura no cuenta como repetida.
     */
    public String validate(UUID userId, Invoice draft, UUID keepId) {
        // Servicio y documento de otro usuario responden igual que inexistentes (RN-002, RN-003)
        String serviceName = services.findByIdForUser(userId, draft.getServiceId())
                .map(Service::getName)
                .orElseThrow(() -> new ServiceException(UNKNOWN_SERVICE));
        if (draft.getDocumentId() != null && documents.findByIdForUser(userId, draft.getDocumentId()).isEmpty()) {
            throw new ServiceException(UNKNOWN_DOCUMENT);
        }
        if (!CURRENCIES.contains(draft.getCurrency())) {
            throw new ServiceException(INVALID_CURRENCY);
        }
        if (draft.getIssueDate() != null && draft.getDueDate().isBefore(draft.getIssueDate())) {
            throw new ServiceException(INVALID_DATES);
        }
        // RN-005: la base también lo impide (índice único), acá se responde con un mensaje claro
        String number = clean(draft.getInvoiceNumber());
        if (number != null) {
            invoices.findConfirmedByNumber(userId, draft.getServiceId(), number)
                    .filter(existing -> !existing.getId().equals(keepId))
                    .ifPresent(existing -> {
                        throw new ConflictException(DUPLICATE_NUMBER.formatted(number));
                    });
        }
        return serviceName;
    }

    /** Textos sin espacios de más; los vacíos se guardan como null. */
    public static Invoice clean(Invoice draft) {
        return draft.toBuilder()
                .invoiceNumber(clean(draft.getInvoiceNumber()))
                .billingPeriod(clean(draft.getBillingPeriod()))
                .paymentCode(clean(draft.getPaymentCode()))
                .build();
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
