package org.acme.card.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

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
) {}
