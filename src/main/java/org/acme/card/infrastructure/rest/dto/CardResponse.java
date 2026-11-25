package org.acme.card.infrastructure.rest.dto;

import org.acme.card.domain.model.CardBrand;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "CardResponse")
public record CardResponse(
        UUID id,
        @Schema(description = "Servicio CREDIT_CARD del emisor") UUID serviceId,
        @Schema(example = "Visa Galicia") String name,
        CardBrand brand,
        @Schema(description = "Últimos 4 de la tarjeta", example = "1234") String lastFour,
        @Schema(description = "Últimos 4 del número de cuenta", example = "4521") String accountLastFour,
        String holderName,
        boolean additional,
        @Schema(example = "27") Integer closingDay,
        @Schema(example = "9") Integer dueDay,
        BigDecimal creditLimit,
        @Schema(example = "ARS") String currency,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
