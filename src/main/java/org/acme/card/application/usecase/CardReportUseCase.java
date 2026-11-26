package org.acme.card.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.card.application.model.PendingInstallments;
import org.acme.card.application.model.SpendingFilter;
import org.acme.card.application.port.output.CardSpendingRepository;
import org.acme.card.domain.model.CardSpendingReport;
import org.acme.card.domain.model.CardTransaction;
import org.acme.card.domain.model.CurrencyTotal;
import org.acme.card.domain.model.PendingInstallment;
import org.acme.domain.exceptions.ServiceException;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Informe de consumos (CU-017) y cuotas pendientes. Solo resúmenes confirmados (RN-017), monedas por separado (RN-018). */
@ApplicationScoped
public class CardReportUseCase {

    static final int TOP_MERCHANTS = 10;
    /** Sin período: los últimos 6 meses de facturación. */
    static final int DEFAULT_MONTHS = 6;

    private final CardSpendingRepository spending;
    private final Clock clock;

    @Inject
    public CardReportUseCase(CardSpendingRepository spending, Clock clock) {
        this.spending = spending;
        this.clock = clock;
    }

    public CardSpendingReport report(UUID userId, SpendingFilter requested) {
        SpendingFilter filter = withPeriod(requested);
        PendingInstallments pending = pending(userId);
        return new CardSpendingReport(filter.from(), filter.to(), spending.totals(userId, filter), spending.byCategory(userId, filter),
                spending.byMonth(userId, filter), spending.byCard(userId, filter), spending.topMerchants(userId, filter, TOP_MERCHANTS),
                pending.items(), pending.totals());
    }

    public PageResult<CardTransaction> transactions(UUID userId, SpendingFilter requested, PageQuery page) {
        return spending.transactions(userId, withPeriod(requested), page);
    }

    public PendingInstallments pending(UUID userId) {
        List<PendingInstallment> items = spending.pendingInstallments(userId);
        return new PendingInstallments(items, totals(items));
    }

    /** Lo que falta pagar por moneda, pesos primero. */
    static List<CurrencyTotal> totals(List<PendingInstallment> items) {
        return items.stream().map(PendingInstallment::currency).distinct()
                .sorted(Comparator.comparing((String c) -> !"ARS".equals(c)).thenComparing(c -> c))
                .map(c -> {
                    List<PendingInstallment> in = items.stream().filter(i -> c.equals(i.currency())).toList();
                    return new CurrencyTotal(c, in.stream().map(PendingInstallment::remainingAmount).reduce(BigDecimal.ZERO, BigDecimal::add), in.size());
                }).toList();
    }

    /** Meses completos: desde el primer día del mes from hasta el primer día del mes to. */
    private SpendingFilter withPeriod(SpendingFilter filter) {
        LocalDate thisMonth = LocalDate.now(clock).withDayOfMonth(1);
        LocalDate to = filter.to() == null ? thisMonth : filter.to().withDayOfMonth(1);
        LocalDate from = filter.from() == null ? to.minusMonths(DEFAULT_MONTHS - 1L) : filter.from().withDayOfMonth(1);
        if (from.isAfter(to)) {
            throw new ServiceException("El mes desde no puede ser posterior al mes hasta");
        }
        return new SpendingFilter(from, to, filter.cardId(), filter.category(), filter.merchant());
    }
}
