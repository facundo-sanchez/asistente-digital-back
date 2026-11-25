package org.acme.card.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** Gasto de una tarjeta en una moneda. */
public record CardSpending(UUID cardId, String cardName, String currency, BigDecimal total, long count) {}
