package org.acme.invoice.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.invoice.application.model.InvoiceView;
import org.acme.invoice.application.port.output.InvoiceRepository;
import org.acme.invoice.domain.model.Invoice;
import org.acme.invoice.domain.model.InvoiceStatus;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationSummary;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Corregir o anular una factura. Su vencimiento se actualiza o se cancela con ella; si ya tiene
 * pagos no se toca: primero se anulan los pagos (RN-008).
 */
@ApplicationScoped
public class ManageInvoiceUseCase {

    static final String NOT_EDITABLE = "Una factura anulada no se puede editar";
    static final String HAS_PAYMENTS = "Su vencimiento tiene pagos registrados: anulalos antes de cambiar la factura";
    static final String CANCEL_REASON = "Factura anulada";

    private final InvoiceRepository invoices;
    private final ObligationRepository obligations;
    private final InvoiceDraftValidator validator;
    private final Clock clock;

    @Inject
    public ManageInvoiceUseCase(InvoiceRepository invoices, ObligationRepository obligations,
                                InvoiceDraftValidator validator, Clock clock) {
        this.invoices = invoices;
        this.obligations = obligations;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional
    public InvoiceView update(UUID userId, UUID invoiceId, Invoice draft) {
        Invoice invoice = find(userId, invoiceId);
        if (invoice.getStatus() == InvoiceStatus.REJECTED) {
            throw new ServiceException(NOT_EDITABLE);
        }
        ObligationSummary current = obligationOf(userId, invoice);
        String serviceName = validator.validate(userId, draft, invoice.getId());

        Invoice changes = InvoiceDraftValidator.clean(draft);
        invoice.setServiceId(changes.getServiceId());
        invoice.setDocumentId(changes.getDocumentId());
        invoice.setInvoiceNumber(changes.getInvoiceNumber());
        invoice.setBillingPeriod(changes.getBillingPeriod());
        invoice.setIssueDate(changes.getIssueDate());
        invoice.setDueDate(changes.getDueDate());
        invoice.setTotalAmount(changes.getTotalAmount());
        invoice.setCurrency(changes.getCurrency());
        invoice.setPaymentCode(changes.getPaymentCode());
        Invoice saved = invoices.save(invoice);

        ObligationSummary obligation = null;
        if (current != null) {
            // El vencimiento copia los datos nuevos (título, servicio, importe, moneda y fecha)
            Obligation updated = current.obligation();
            Obligation fresh = RegisterInvoiceUseCase.obligationFor(saved, serviceName);
            updated.setTitle(fresh.getTitle());
            updated.setServiceId(fresh.getServiceId());
            updated.setAmount(fresh.getAmount());
            updated.setCurrency(fresh.getCurrency());
            updated.setDueDate(fresh.getDueDate());
            obligation = obligations.save(updated);
        }
        return new InvoiceView(saved, serviceName, obligation);
    }

    /** Anula la factura y cancela su vencimiento. Queda en el historial. */
    @Transactional
    public void reject(UUID userId, UUID invoiceId) {
        Invoice invoice = find(userId, invoiceId);
        ObligationSummary current = obligationOf(userId, invoice);
        invoice.reject();
        invoices.save(invoice);
        if (current != null) {
            Obligation obligation = current.obligation();
            obligation.cancel(CANCEL_REASON, Instant.now(clock));
            obligations.save(obligation);
        }
    }

    /** Su vencimiento, si lo tiene; con pagos vigentes no se puede tocar. */
    private ObligationSummary obligationOf(UUID userId, Invoice invoice) {
        ObligationSummary summary = obligations.findByInvoices(userId, Set.of(invoice.getId())).get(invoice.getId());
        if (summary != null && summary.paidAmount().signum() > 0) {
            throw new ServiceException(HAS_PAYMENTS);
        }
        return summary;
    }

    /** Una factura de otro usuario responde igual que una inexistente. */
    private Invoice find(UUID userId, UUID invoiceId) {
        return invoices.findByIdForUser(userId, invoiceId).orElseThrow(() -> new NotFoundException("factura"));
    }
}
