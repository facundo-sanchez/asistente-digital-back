package org.acme.payment.application.port.output;

import org.acme.payment.domain.model.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Pagos de un usuario. No se borran: se anulan (RN-008). */
public interface PaymentRepository {

    /** Inserta si no tiene id; si no, actualiza. */
    Payment save(Payment payment);

    /** Del usuario, anulado o no. */
    Optional<Payment> findByIdForUser(UUID userId, UUID paymentId);

    /** Anulados incluidos; por fecha de pago, más nuevos primero. */
    List<Payment> findForObligation(UUID userId, UUID obligationId);
}
