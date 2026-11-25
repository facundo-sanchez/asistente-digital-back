package org.acme.obligation.domain.model;

import java.math.BigDecimal;

/**
 * Modelo de lectura: obligación + importes pagados + estado efectivo.
 * Proviene de la vista v_obligations.
 *
 * @param paidAmount      suma de pagos no anulados
 * @param balance         saldo pendiente (null si la obligación no tiene importe)
 * @param effectiveStatus estado calculado por fecha
 */
public record ObligationSummary(
        Obligation obligation,
        BigDecimal paidAmount,
        BigDecimal balance,
        EffectiveObligationStatus effectiveStatus
) {}
