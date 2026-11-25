package org.acme.card.domain.model;

import java.math.BigDecimal;

/** Gasto con tarjeta de una categoría en una moneda. */
public record CategorySpending(SpendingCategory category, String currency, BigDecimal total, long count) {}
