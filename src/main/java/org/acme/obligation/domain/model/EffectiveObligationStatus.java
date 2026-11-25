package org.acme.obligation.domain.model;

/**
 * Estado que ve el usuario (CU-005). Lo calcula la vista v_obligations
 * según la fecha de vencimiento y la zona horaria del usuario (RN-012).
 * DUE_SOON = vence en los próximos 7 días.
 */
public enum EffectiveObligationStatus {
    PENDING, PARTIALLY_PAID, DUE_SOON, OVERDUE, PAID, CANCELLED
}
