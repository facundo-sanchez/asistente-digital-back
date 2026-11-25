package org.acme.card.infrastructure.rest.dto;

import org.acme.card.domain.model.SpendingCategory;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * GET /cards/report. Todos los totales son por moneda y no se convierten (RN-018).
 * Solo incluye consumos de resúmenes confirmados (RN-017).
 */
@Schema(name = "CardSpendingReportResponse")
public record CardSpendingReportResponse(
        LocalDate from,
        LocalDate to,
        List<CurrencyTotalResponse> totals,
        List<CategorySpendingResponse> byCategory,
        List<MonthlySpendingResponse> byMonth,
        List<CardSpendingResponse> byCard,
        List<MerchantSpendingResponse> topMerchants,
        List<PendingInstallmentResponse> pendingInstallments,
        List<CurrencyTotalResponse> pendingInstallmentsTotals
) {
    @Schema(name = "CurrencyTotalResponse")
    public record CurrencyTotalResponse(
            @Schema(example = "ARS") String currency,
            @Schema(example = "1850000.00") BigDecimal total,
            @Schema(example = "142") long count
    ) {}

    @Schema(name = "CategorySpendingResponse")
    public record CategorySpendingResponse(
            SpendingCategory category,
            String currency,
            BigDecimal total,
            long count
    ) {}

    @Schema(name = "MonthlySpendingResponse")
    public record MonthlySpendingResponse(
            @Schema(description = "Primer día del mes de facturación", example = "2026-11-01") LocalDate month,
            String currency,
            BigDecimal total,
            long count
    ) {}

    @Schema(name = "CardSpendingResponse")
    public record CardSpendingResponse(
            UUID cardId,
            @Schema(example = "Visa Galicia") String cardName,
            String currency,
            BigDecimal total,
            long count
    ) {}

    @Schema(name = "MerchantSpendingResponse")
    public record MerchantSpendingResponse(
            @Schema(example = "SUPERMERCADO X") String merchant,
            String currency,
            BigDecimal total,
            long count
    ) {}
}
