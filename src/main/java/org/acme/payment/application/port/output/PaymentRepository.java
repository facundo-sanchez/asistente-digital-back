package org.acme.payment.application.port.output;

import org.acme.payment.domain.model.Payment;

/** Pagos de un usuario. No se borran: se anulan (RN-008). */
public interface PaymentRepository {

    /** Inserta si no tiene id; si no, actualiza. */
    Payment save(Payment payment);
}
