package org.acme.card.application.port.output;

import org.acme.card.application.model.SpendingFilter;
import org.acme.card.domain.model.CardSpending;
import org.acme.card.domain.model.CardTransaction;
import org.acme.card.domain.model.CategorySpending;
import org.acme.card.domain.model.CurrencyTotal;
import org.acme.card.domain.model.MerchantSpending;
import org.acme.card.domain.model.MonthlySpending;
import org.acme.card.domain.model.PendingInstallment;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.util.List;
import java.util.UUID;

/** Sumas sobre v_card_spending (resúmenes confirmados, sin pagos) y las cuotas pendientes. Todo por moneda. */
public interface CardSpendingRepository {

    List<CurrencyTotal> totals(UUID userId, SpendingFilter filter);

    List<CategorySpending> byCategory(UUID userId, SpendingFilter filter);

    List<MonthlySpending> byMonth(UUID userId, SpendingFilter filter);

    List<CardSpending> byCard(UUID userId, SpendingFilter filter);

    List<MerchantSpending> topMerchants(UUID userId, SpendingFilter filter, int limit);

    /** Los consumos que suman en el informe, los más nuevos primero. */
    PageResult<CardTransaction> transactions(UUID userId, SpendingFilter filter, PageQuery page);

    List<PendingInstallment> pendingInstallments(UUID userId);
}
