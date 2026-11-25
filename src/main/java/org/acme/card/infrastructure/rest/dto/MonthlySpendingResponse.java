package org.acme.card.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "MonthlySpendingResponse")
public record MonthlySpendingResponse(
        @Schema(description = "Primer día del mes de facturación", example = "2026-11-01") LocalDate month,
        String currency,
        BigDecimal total,
        long count
) {}
