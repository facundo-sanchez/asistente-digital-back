package org.acme.card.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Gasto con tarjeta de un mes en una moneda.
 *
 * @param month primer día del mes de facturación (fecha de cierre del resumen)
 */
public record MonthlySpending(LocalDate month, String currency, BigDecimal total, long count) {}
