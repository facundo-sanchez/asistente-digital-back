package org.acme.payment.application.usecase;

import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.payment.application.port.output.PaymentRepository;
import org.acme.payment.domain.model.Payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Pagos en memoria. Suma cada pago vigente al "pagado" de la obligación, como hace la vista v_obligations. */
public class InMemoryPaymentRepository implements PaymentRepository {

    final List<Payment> rows = new ArrayList<>();
    private final InMemoryObligationRepository obligations;

    public InMemoryPaymentRepository(InMemoryObligationRepository obligations) {
        this.obligations = obligations;
    }

    @Override
    public Payment save(Payment payment) {
        Payment copy = payment.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
        }
        rows.removeIf(p -> p.getId().equals(copy.getId()));
        rows.add(copy);
        BigDecimal paid = rows.stream()
                .filter(p -> p.getObligationId().equals(copy.getObligationId()) && !p.isVoided())
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        obligations.paid.put(copy.getObligationId(), paid);
        return copy.toBuilder().build();
    }

    @Override
    public Optional<Payment> findByIdForUser(UUID userId, UUID paymentId) {
        return rows.stream().filter(p -> p.getId().equals(paymentId) && p.getUserId().equals(userId)).findFirst().map(p -> p.toBuilder().build());
    }

    @Override
    public List<Payment> findForObligation(UUID userId, UUID obligationId) {
        return rows.stream()
                .filter(p -> p.getUserId().equals(userId) && p.getObligationId().equals(obligationId))
                .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                .map(p -> p.toBuilder().build())
                .toList();
    }
}
