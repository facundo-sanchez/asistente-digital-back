package org.acme.dashboard.application.model;

import java.math.BigDecimal;

/** Lo del mes en una moneda: total de los vencimientos, lo pagado y lo que falta (RN-018: una fila por moneda). */
public record MonthTotals(String currency, BigDecimal total, BigDecimal paid, BigDecimal pending) {}
