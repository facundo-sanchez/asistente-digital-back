package org.acme.card.domain.model;

import java.math.BigDecimal;

/** Total y cantidad de movimientos en una moneda. */
public record CurrencyTotal(String currency, BigDecimal total, long count) {}
