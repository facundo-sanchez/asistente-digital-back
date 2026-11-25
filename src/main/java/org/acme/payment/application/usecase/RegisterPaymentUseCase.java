package org.acme.payment.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.payment.application.port.output.PaymentRepository;
import org.acme.payment.domain.model.Payment;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Registrar el pago total o parcial de una obligación (CU-006). Actualiza su estado en la misma transacción. */
@ApplicationScoped
public class RegisterPaymentUseCase {

    static final String CANCELLED = "La obligación está cancelada";
    static final String ALREADY_PAID = "La obligación ya está pagada";
    static final String WRONG_CURRENCY = "El pago tiene que ser en %s, la moneda de la obligación";
    static final String INVALID_CURRENCY = "La moneda tiene que ser ARS o USD";
    static final String UNKNOWN_RECEIPT = "El comprobante elegido no existe";

    private static final Set<String> CURRENCIES = Set.of("ARS", "USD");

    private final PaymentRepository payments;
    private final ObligationRepository obligations;
    private final DocumentRepository documents;

    @Inject
    public RegisterPaymentUseCase(PaymentRepository payments, ObligationRepository obligations, DocumentRepository documents) {
        this.payments = payments;
        this.obligations = obligations;
        this.documents = documents;
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
        if (obligation.getCurrency() != null && !obligation.getCurrency().equals(draft.getCurrency())) {
            throw new ServiceException(WRONG_CURRENCY.formatted(obligation.getCurrency()));
        }
        if (!CURRENCIES.contains(draft.getCurrency())) {
            throw new ServiceException(INVALID_CURRENCY);
        }
        // Un documento de otro usuario o borrado responde igual que uno inexistente
        if (draft.getReceiptDocumentId() != null && documents.findByIdForUser(userId, draft.getReceiptDocumentId()).isEmpty()) {
            throw new ServiceException(UNKNOWN_RECEIPT);
        }

        Payment payment = payments.save(draft.toBuilder()
                .id(null)
                .userId(userId)
                .notes(draft.getNotes() == null || draft.getNotes().isBlank() ? null : draft.getNotes().trim())
                .build());
        obligation.applyPaidAmount(summary.paidAmount().add(payment.getAmount()), Instant.now());
        obligations.save(obligation);
        return payment;
    }
}
