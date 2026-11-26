package org.acme.card.infrastructure.rest.dto;

import org.acme.card.domain.model.CardStatementSource;
import org.acme.card.domain.model.CardStatementStatus;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "CardStatementResponse")
public record CardStatementResponse(
        UUID id,
        UUID cardId,
        @Schema(description = "Nombre de la tarjeta", example = "Visa Galicia") String cardName,
        UUID documentId,
        UUID extractionResultId,
        @Schema(description = "Obligación de pago en moneda local") UUID obligationId,
        @Schema(description = "Estado del vencimiento en pesos") EffectiveObligationStatus obligationStatus,
        @Schema(description = "Obligación de pago en dólares") UUID obligationUsdId,
        @Schema(description = "Estado del vencimiento en dólares") EffectiveObligationStatus obligationUsdStatus,
        LocalDate periodStart,
        LocalDate closingDate,
        LocalDate dueDate,
        LocalDate nextClosingDate,
        LocalDate nextDueDate,
        @Schema(example = "ARS") String currency,
        @Schema(example = "315000.00") BigDecimal totalAmount,
        @Schema(example = "12.99") BigDecimal totalAmountUsd,
        @Schema(example = "45000.00") BigDecimal minimumPayment,
        CardStatementStatus status,
        CardStatementSource source,
        Instant confirmedAt,
        Instant createdAt,
        Instant updatedAt
) {}
