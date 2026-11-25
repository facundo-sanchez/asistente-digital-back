package org.acme.payment.application.usecase;

import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.payment.application.port.output.PaymentRepository;
import org.acme.payment.domain.model.Payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
}
