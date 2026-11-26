package org.acme.card.application.model;

import java.math.BigDecimal;

/** Una tarjeta del resumen tal como la leyó la IA (titular o adicional, con su total si figura). */
public record ExtractedCard(String lastFour, String holderName, BigDecimal total, BigDecimal totalUsd) {}
