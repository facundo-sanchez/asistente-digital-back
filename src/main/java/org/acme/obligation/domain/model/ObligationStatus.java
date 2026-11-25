package org.acme.obligation.domain.model;

/** Estado persistido. "Próxima a vencer" y "vencida" se calculan: ver {@link EffectiveObligationStatus}. */
public enum ObligationStatus {
    PENDING, PARTIALLY_PAID, PAID, CANCELLED
}
