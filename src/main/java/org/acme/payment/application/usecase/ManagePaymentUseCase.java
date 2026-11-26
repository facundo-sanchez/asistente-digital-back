package org.acme.payment.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.payment.application.port.output.PaymentRepository;
import org.acme.payment.domain.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Historial, anulación y corrección de pagos (RN-008). Los pagos no se borran ni se editan:
 * corregir = anular el pago y crear otro que lo reemplaza. El estado de la obligación se recalcula.
 */
@ApplicationScoped
public class ManagePaymentUseCase {

    static final String CORRECTED = "Corregido";

    private final PaymentRepository payments;
    private final ObligationRepository obligations;
    private final PaymentDraftValidator validator;

    @Inject
    public ManagePaymentUseCase(PaymentRepository payments, ObligationRepository obligations, PaymentDraftValidator validator) {
        this.payments = payments;
        this.obligations = obligations;
        this.validator = validator;
    }

    /** Todos los pagos de la obligación, anulados incluidos, más nuevos primero. */
    public List<Payment> history(UUID userId, UUID obligationId) {
        findObligation(userId, obligationId);
        return payments.findForObligation(userId, obligationId);
    }

    @Transactional
    public void voidPayment(UUID userId, UUID paymentId, String reason) {
        Payment payment = findPayment(userId, paymentId);
        ObligationSummary summary = findObligation(userId, payment.getObligationId());

        payment.voidPayment(reason == null ? null : reason.trim(), Instant.now());
        payments.save(payment);
        // Lo pagado se calcula con el resumen leído antes: la vista ya leída en la transacción no se actualiza sola
        applyPaid(summary, summary.paidAmount().subtract(payment.getAmount()));
    }

    @Transactional
    public Payment correct(UUID userId, UUID paymentId, Payment draft) {
        Payment previous = findPayment(userId, paymentId);
        ObligationSummary summary = findObligation(userId, previous.getObligationId());
        validator.validate(userId, summary.obligation(), draft);

        previous.voidPayment(CORRECTED, Instant.now());
        payments.save(previous);
        Payment replacement = payments.save(PaymentDraftValidator.clean(userId, previous.getObligationId(), draft)
                .toBuilder().replacesPaymentId(previous.getId()).build());
        applyPaid(summary, summary.paidAmount().subtract(previous.getAmount()).add(replacement.getAmount()));
        return replacement;
    }

    private void applyPaid(ObligationSummary summary, BigDecimal paid) {
        Obligation obligation = summary.obligation();
        obligation.applyPaidAmount(paid, Instant.now());
        obligations.save(obligation);
    }

    /** Un pago de otro usuario responde igual que uno inexistente. */
    private Payment findPayment(UUID userId, UUID paymentId) {
        return payments.findByIdForUser(userId, paymentId).orElseThrow(() -> new NotFoundException("pago"));
    }

    private ObligationSummary findObligation(UUID userId, UUID obligationId) {
        return obligations.findByIdForUser(userId, obligationId).orElseThrow(() -> new NotFoundException("obligación"));
    }
}
