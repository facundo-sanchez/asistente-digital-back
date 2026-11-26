package org.acme.dashboard.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.dashboard.application.model.DashboardSummary;
import org.acme.dashboard.application.model.MonthTotals;
import org.acme.dashboard.application.model.OverdueTotals;
import org.acme.obligation.application.model.ObligationFilter;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.service.application.model.ServiceFilter;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.shared.application.model.PageQuery;
import org.acme.user.application.port.output.UserRepository;
import org.acme.user.domain.model.User;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** Los números del Inicio: el mes del usuario, lo vencido, lo que vence pronto y sus servicios activos. */
@ApplicationScoped
public class DashboardUseCase {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ObligationRepository obligations;
    private final ServiceRepository services;
    private final UserRepository users;
    private final Clock clock;

    @Inject
    public DashboardUseCase(ObligationRepository obligations, ServiceRepository services, UserRepository users, Clock clock) {
        this.obligations = obligations;
        this.services = services;
        this.users = users;
        this.clock = clock;
    }

    public DashboardSummary summary(UUID userId) {
        LocalDate today = LocalDate.now(clock.withZone(zone(userId)));
        LocalDate start = today.withDayOfMonth(1);
        LocalDate end = today.with(TemporalAdjusters.lastDayOfMonth());

        List<ObligationSummary> month = obligations.findDueBetween(userId, start, end);
        // shortcut: lo vencido se toma de a 100; con más, sumarlo en la base
        List<ObligationSummary> overdue = obligations.findForUser(userId, onlyStatus(EffectiveObligationStatus.OVERDUE), new PageQuery(0, PageQuery.MAX_SIZE)).content();
        long dueSoon = obligations.findForUser(userId, onlyStatus(EffectiveObligationStatus.DUE_SOON), new PageQuery(0, 1)).totalElements();
        long activeServices = services.findForUser(userId, new ServiceFilter(null, null, true), new PageQuery(0, 1)).totalElements();

        return new DashboardSummary(start, end, monthTotals(month), month.stream().filter(o -> o.obligation().getAmount() == null).count(),
                overdueTotals(overdue), dueSoon, activeServices);
    }

    /** Por moneda: total, pagado y lo que falta de los vencimientos del mes que tienen importe. */
    static List<MonthTotals> monthTotals(List<ObligationSummary> month) {
        return currencies(month).stream().map(currency -> {
            List<ObligationSummary> rows = inCurrency(month, currency);
            return new MonthTotals(currency, sum(rows, o -> o.obligation().getAmount()), sum(rows, ObligationSummary::paidAmount),
                    sum(rows, ObligationSummary::balance));
        }).toList();
    }

    static List<OverdueTotals> overdueTotals(List<ObligationSummary> overdue) {
        return currencies(overdue).stream().map(currency -> {
            List<ObligationSummary> rows = inCurrency(overdue, currency);
            return new OverdueTotals(currency, rows.size(), sum(rows, ObligationSummary::balance));
        }).toList();
    }

    /** Las monedas presentes, pesos primero. Los vencimientos sin importe no tienen moneda. */
    private static List<String> currencies(List<ObligationSummary> rows) {
        return rows.stream().map(o -> o.obligation().getCurrency()).filter(Objects::nonNull).distinct()
                .sorted(Comparator.comparing((String c) -> !"ARS".equals(c)).thenComparing(c -> c))
                .toList();
    }

    private static List<ObligationSummary> inCurrency(List<ObligationSummary> rows, String currency) {
        return rows.stream().filter(o -> currency.equals(o.obligation().getCurrency())).toList();
    }

    private static BigDecimal sum(List<ObligationSummary> rows, Function<ObligationSummary, BigDecimal> value) {
        return rows.stream().map(value).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static ObligationFilter onlyStatus(EffectiveObligationStatus status) {
        return new ObligationFilter(null, Set.of(status), null, null, null, null);
    }

    private ZoneId zone(UUID userId) {
        try {
            return users.findById(userId).map(User::getTimezone).map(ZoneId::of).orElse(DEFAULT_ZONE);
        } catch (RuntimeException e) {
            return DEFAULT_ZONE;
        }
    }
}
