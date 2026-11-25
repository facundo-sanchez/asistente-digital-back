package org.acme.card.domain.model;

import java.math.BigDecimal;

/** Gasto en un comercio en una moneda. */
public record MerchantSpending(String merchant, String currency, BigDecimal total, long count) {}
