package org.acme.payment.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.payment.application.port.output.PaymentRepository;
import org.acme.payment.domain.model.Payment;

import java.time.Instant;
import java.util.UUID;

/** Registrar el pago total o parcial de una obligación (CU-006). Actualiza su estado en la misma transacción. */
@ApplicationScoped
public class RegisterPaymentUseCase {

    static final String CANCELLED = "La obligación está cancelada";
    static final String ALREADY_PAID = "La obligación ya está pagada";

    private final PaymentRepository payments;
    private final ObligationRepository obligations;
    private final PaymentDraftValidator validator;

    @Inject
    public RegisterPaymentUseCase(PaymentRepository payments, ObligationRepository obligations, PaymentDraftValidator validator) {
        this.payments = payments;
        this.obligations = obligations;
        this.validator = validator;
    }

    // shortcut: dos pagos simultáneos de la misma obligación podrían calcular el estado con el saldo previo;
    // con un usuario por cuenta no pasa. Si hiciera falta, bloquear la fila de la obligación (SELECT ... FOR UPDATE).
    @Transactional
    public Payment register(UUID userId, Payment draft) {
        ObligationSummary summary = obligations.findByIdForUser(userId, draft.getObligationId())
                .orElseThrow(() -> new NotFoundException("obligación"));
        Obligation obligation = summary.obligation();
        if (obligation.getStatus() == ObligationStatus.CANCELLED) {
            throw new ServiceException(CANCELLED);
        }
        if (obligation.getStatus() == ObligationStatus.PAID) {
            throw new ServiceException(ALREADY_PAID);
        }
        validator.validate(userId, obligation, draft);

        Payment payment = payments.save(PaymentDraftValidator.clean(userId, obligation.getId(), draft));
        obligation.applyPaidAmount(summary.paidAmount().add(payment.getAmount()), Instant.now());
        obligations.save(obligation);
        return payment;
    }
}
