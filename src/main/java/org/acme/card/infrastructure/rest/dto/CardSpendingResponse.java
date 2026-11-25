package org.acme.card.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "CardSpendingResponse")
public record CardSpendingResponse(
        UUID cardId,
        @Schema(example = "Visa Galicia") String cardName,
        String currency,
        BigDecimal total,
        long count
) {}
