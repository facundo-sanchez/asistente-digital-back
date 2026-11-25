package org.acme.card.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
) {
    public record CurrencyTotal(String currency, BigDecimal total, long count) {}

    public record CategorySpending(SpendingCategory category, String currency, BigDecimal total, long count) {}

    /** month = primer día del mes de facturación (fecha de cierre del resumen). */
    public record MonthlySpending(LocalDate month, String currency, BigDecimal total, long count) {}

    public record CardSpending(UUID cardId, String cardName, String currency, BigDecimal total, long count) {}

    public record MerchantSpending(String merchant, String currency, BigDecimal total, long count) {}
}
