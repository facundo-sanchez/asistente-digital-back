package org.acme.invoice.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.invoice.application.model.InvoiceView;
import org.acme.invoice.application.port.output.InvoiceRepository;
import org.acme.invoice.domain.model.Invoice;
import org.acme.invoice.domain.model.InvoiceSource;
import org.acme.invoice.domain.model.InvoiceStatus;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.obligation.domain.model.ObligationType;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Cargar una factura a mano (CU-002). El usuario ya revisó los datos al tipearlos: queda
 * confirmada y genera su vencimiento en la misma transacción (RN-004).
 */
@ApplicationScoped
public class RegisterInvoiceUseCase {

    private final InvoiceRepository invoices;
    private final ObligationRepository obligations;
    private final InvoiceDraftValidator validator;
    private final Clock clock;

    @Inject
    public RegisterInvoiceUseCase(InvoiceRepository invoices, ObligationRepository obligations,
                                  InvoiceDraftValidator validator, Clock clock) {
        this.invoices = invoices;
        this.obligations = obligations;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional
    public InvoiceView register(UUID userId, Invoice draft) {
        String serviceName = validator.validate(userId, draft, null);
        Invoice invoice = InvoiceDraftValidator.clean(draft).toBuilder()
                .id(null)
                .userId(userId)
                .status(InvoiceStatus.PENDING_VALIDATION)
                // Manual salvo que venga de una propuesta de la IA (con su extractionResultId)
                .source(draft.getSource() == null ? InvoiceSource.MANUAL : draft.getSource())
                .build();
        invoice.confirm(Instant.now(clock));
        Invoice saved = invoices.save(invoice);
        ObligationSummary obligation = obligations.save(obligationFor(saved, serviceName));
        return new InvoiceView(saved, serviceName, obligation);
    }

    /** El vencimiento de una factura: «Luz casa · factura 2026-12» (o el número si no hay período). */
    static Obligation obligationFor(Invoice invoice, String serviceName) {
        String reference = invoice.getBillingPeriod() != null ? invoice.getBillingPeriod() : invoice.getInvoiceNumber();
        return Obligation.builder()
                .userId(invoice.getUserId())
                .serviceId(invoice.getServiceId())
                .invoiceId(invoice.getId())
                .title(serviceName + " · factura" + (reference == null ? "" : " " + reference))
                .obligationType(ObligationType.INVOICE_PAYMENT)
                .amount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .dueDate(invoice.getDueDate())
                .build();
    }
}
