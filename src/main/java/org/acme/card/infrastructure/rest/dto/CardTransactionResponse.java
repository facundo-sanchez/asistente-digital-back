package org.acme.card.infrastructure.rest.dto;

import org.acme.card.domain.model.CardTransactionType;
import org.acme.card.domain.model.CategorySource;
import org.acme.card.domain.model.SpendingCategory;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "CardTransactionResponse")
public record CardTransactionResponse(
        UUID id,
        UUID cardId,
        UUID statementId,
        LocalDate transactionDate,
        @Schema(example = "TIENDA Y C.03/06") String description,
        @Schema(example = "TIENDA Y") String merchantName,
        SpendingCategory category,
        CategorySource categorySource,
        CardTransactionType transactionType,
        @Schema(description = "Con signo: + cargo, - crédito", example = "18000.00") BigDecimal amount,
        @Schema(example = "ARS") String currency,
        @Schema(example = "3") Integer installmentNumber,
        @Schema(example = "6") Integer installmentTotal,
        @Schema(example = "108000.00") BigDecimal originalAmount,
        UUID purchaseGroupId,
        String reference,
        BigDecimal confidenceScore,
        Instant createdAt,
        Instant updatedAt
) {}
