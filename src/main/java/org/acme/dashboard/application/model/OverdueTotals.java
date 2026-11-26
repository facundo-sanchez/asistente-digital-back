package org.acme.dashboard.application.model;

import java.math.BigDecimal;

/** Lo vencido sin pagar en una moneda: cuántos vencimientos y cuánto falta. */
public record OverdueTotals(String currency, long count, BigDecimal balance) {}
