package org.acme.card.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Informe de compras con tarjeta (CU-017). Se calcula sobre v_card_spending:
 * solo resúmenes confirmados y sin pagos del resumen. Cada total es por moneda (RN-018).
 */
public record CardSpendingReport(
        LocalDate from,
        LocalDate to,
        List<CurrencyTotal> totals,
        List<CategorySpending> byCategory,
        List<MonthlySpending> byMonth,
        List<CardSpending> byCard,
        List<MerchantSpending> topMerchants,
        List<PendingInstallment> pendingInstallments,
        List<CurrencyTotal> pendingInstallmentsTotals
) {}
