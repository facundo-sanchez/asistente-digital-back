package org.acme.dashboard.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.dashboard.application.model.DashboardSpending;
import org.acme.dashboard.application.model.DashboardSummary;
import org.acme.dashboard.application.model.ExpiringDocument;
import org.acme.dashboard.application.model.MonthTotals;
import org.acme.dashboard.application.model.OverdueTotals;
import org.acme.dashboard.application.model.PendingReview;
import org.acme.dashboard.application.model.SpendingTotal;
import org.acme.dashboard.application.port.output.SpendingRepository;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.application.port.output.ExtractionResultRepository;
import org.acme.document.domain.model.Document;
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
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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
    private final DocumentRepository documents;
    private final ExtractionResultRepository extractions;
    private final SpendingRepository spending;
    private final Clock clock;

    @Inject
    public DashboardUseCase(ObligationRepository obligations, ServiceRepository services, UserRepository users, DocumentRepository documents,
                            ExtractionResultRepository extractions, SpendingRepository spending, Clock clock) {
        this.obligations = obligations;
        this.services = services;
        this.users = users;
        this.documents = documents;
        this.extractions = extractions;
        this.spending = spending;
        this.clock = clock;
    }

    public DashboardSummary summary(UUID userId) {
        LocalDate today = today(userId);
        LocalDate start = today.withDayOfMonth(1);
        LocalDate end = today.with(TemporalAdjusters.lastDayOfMonth());

        List<ObligationSummary> month = obligations.findDueBetween(userId, start, end);
        // shortcut: lo vencido se toma de a 100; con más, sumarlo en la base
        List<ObligationSummary> overdue = obligations.findForUser(userId, onlyStatus(EffectiveObligationStatus.OVERDUE), new PageQuery(0, PageQuery.MAX_SIZE)).content();
        long dueSoon = obligations.findForUser(userId, onlyStatus(EffectiveObligationStatus.DUE_SOON), new PageQuery(0, 1)).totalElements();
        long activeServices = services.findForUser(userId, new ServiceFilter(null, null, true), new PageQuery(0, 1)).totalElements();

        return new DashboardSummary(start, end, monthTotals(month), month.stream().filter(o -> o.obligation().getAmount() == null).count(),
                overdueTotals(overdue), dueSoon, activeServices, toReview(userId),
                documents.findExpiringBetween(userId, today, today.plusDays(30)).stream()
                        .map(d -> new ExpiringDocument(d.getId(), title(d), d.getDocumentType(), d.getExpiresOn())).toList());
    }

    /** Lo pagado en los últimos 6 meses (incluido el actual, con ceros) y lo de este mes por categoría. */
    public DashboardSpending spending(UUID userId) {
        YearMonth current = YearMonth.from(today(userId));
        YearMonth first = current.minusMonths(5);
        List<SpendingTotal> paid = spending.paidByMonth(userId, first.atDay(1), current.atEndOfMonth());
        return new DashboardSpending(fillMonths(paid, first, current),
                spending.paidByCategory(userId, current.atDay(1), current.atEndOfMonth()));
    }

    /** Un total por mes y moneda, en orden; los pesos siempre, aunque no haya pagos. */
    static List<SpendingTotal> fillMonths(List<SpendingTotal> paid, YearMonth first, YearMonth last) {
        List<String> currencies = new ArrayList<>(List.of("ARS"));
        paid.stream().map(SpendingTotal::currency).filter(c -> !currencies.contains(c)).distinct().sorted().toList().forEach(currencies::add);
        List<SpendingTotal> months = new ArrayList<>();
        for (String currency : currencies) {
            for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
                String key = m.toString();
                BigDecimal total = paid.stream().filter(p -> p.key().equals(key) && p.currency().equals(currency))
                        .map(SpendingTotal::total).findFirst().orElse(BigDecimal.ZERO);
                months.add(new SpendingTotal(key, currency, total));
            }
        }
        return months;
    }

    private List<PendingReview> toReview(UUID userId) {
        return extractions.findPending(userId).stream().map(r -> new PendingReview(r.getId(), r.getSchemaVersion(), r.getDocumentId(),
                documents.findByIdForUser(userId, r.getDocumentId()).map(DashboardUseCase::title).orElse(null), r.getCreatedAt())).toList();
    }

    private static String title(Document d) {
        return d.getTitle() != null && !d.getTitle().isBlank() ? d.getTitle() : d.getOriginalFilename();
    }

    private LocalDate today(UUID userId) {
        return LocalDate.now(clock.withZone(zone(userId)));
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
