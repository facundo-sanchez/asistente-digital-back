package org.acme.card.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.card.application.model.CardSubscriptions;
import org.acme.card.application.model.SubscriptionCharge;
import org.acme.card.application.port.output.CardSubscriptionRepository;
import org.acme.card.domain.model.CardSubscription;
import org.acme.card.domain.model.CurrencyTotal;
import org.acme.domain.exceptions.ServiceException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Suscripciones en la tarjeta (módulo 18): un comercio con compras sin cuotas en 3 de los últimos 4 resúmenes
 * de la tarjeta, con importes dentro de ±30 % de su mediana. Se calcula al pedirlo, monedas por separado.
 */
@ApplicationScoped
public class CardSubscriptionUseCase {

    static final int CLOSINGS = 4;
    static final int MIN_CLOSINGS = 3;
    static final BigDecimal TOLERANCE = new BigDecimal("0.30");
    static final int INCREASE_FROM = 10;

    private final CardSubscriptionRepository repository;
    private final Clock clock;

    @Inject
    public CardSubscriptionUseCase(CardSubscriptionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public CardSubscriptions list(UUID userId, UUID cardId) {
        Set<String> dismissed = repository.dismissed(userId);
        List<CardSubscription> items = detect(repository.charges(userId, cardId, LocalDate.now(clock).minusMonths(12))).stream()
                .filter(s -> !dismissed.contains(s.merchant())).toList();
        return new CardSubscriptions(items, totals(items), dismissed.stream().sorted().toList());
    }

    public void dismiss(UUID userId, String merchant) {
        repository.dismiss(userId, required(merchant), clock.instant());
    }

    public void restore(UUID userId, String merchant) {
        repository.restore(userId, required(merchant));
    }

    /** "NETFLIX.COM 12345" → "NETFLIX.COM": los resúmenes agregan una referencia distinta cada mes. */
    static String normalize(String merchant) {
        return merchant.trim().toUpperCase(Locale.ROOT).replaceAll("(\\s+[A-Z0-9*#/-]*\\d[A-Z0-9*#/-]*)+$", "");
    }

    static List<CardSubscription> detect(List<SubscriptionCharge> charges) {
        List<CardSubscription> found = new ArrayList<>();
        charges.stream().collect(Collectors.groupingBy(SubscriptionCharge::cardId)).values().forEach(ofCard -> {
            List<LocalDate> closings = ofCard.stream().map(SubscriptionCharge::closingDate).distinct()
                    .sorted(Comparator.reverseOrder()).limit(CLOSINGS).toList();
            ofCard.stream().collect(Collectors.groupingBy(c -> c.currency() + "|" + normalize(c.merchant())))
                    .values().forEach(group -> subscription(group, closings).ifPresent(found::add));
        });
        found.sort(Comparator.comparing(CardSubscription::currency).thenComparing(CardSubscription::lastAmount, Comparator.reverseOrder()));
        return found;
    }

    /** Por resumen se suma lo del comercio; cuentan los resúmenes con importe cerca de la mediana. */
    private static Optional<CardSubscription> subscription(List<SubscriptionCharge> group, List<LocalDate> closings) {
        Map<LocalDate, BigDecimal> perClosing = new TreeMap<>(Comparator.reverseOrder());
        group.stream().filter(c -> closings.contains(c.closingDate()))
                .forEach(c -> perClosing.merge(c.closingDate(), c.amount(), BigDecimal::add));
        if (perClosing.size() < MIN_CLOSINGS) {
            return Optional.empty();
        }
        BigDecimal median = median(perClosing.values());
        BigDecimal margin = median.multiply(TOLERANCE);
        List<Map.Entry<LocalDate, BigDecimal>> counted = perClosing.entrySet().stream()
                .filter(e -> e.getValue().subtract(median).abs().compareTo(margin) <= 0).toList();
        if (counted.size() < MIN_CLOSINGS) {
            return Optional.empty();
        }
        BigDecimal last = counted.get(0).getValue();
        BigDecimal previous = counted.get(1).getValue();
        SubscriptionCharge latest = group.stream().filter(c -> c.closingDate().equals(counted.get(0).getKey()))
                .max(Comparator.comparing(SubscriptionCharge::transactionDate)).orElseThrow();
        LocalDate since = group.stream().map(SubscriptionCharge::transactionDate).min(Comparator.naturalOrder()).orElseThrow();
        int months = (int) group.stream().map(SubscriptionCharge::closingDate).distinct().count();
        return Optional.of(new CardSubscription(latest.cardId(), latest.cardName(), normalize(latest.merchant()), latest.category(),
                latest.currency(), last, latest.transactionDate(), previous, since, months, increase(last, previous)));
    }

    /** % de aumento sobre el importe anterior, solo desde el 10 %. */
    static Integer increase(BigDecimal last, BigDecimal previous) {
        if (previous.signum() <= 0) {
            return null;
        }
        int percent = last.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 0, RoundingMode.HALF_UP).intValue();
        return percent >= INCREASE_FROM ? percent : null;
    }

    private static BigDecimal median(Collection<BigDecimal> values) {
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(mid)
                : sorted.get(mid - 1).add(sorted.get(mid)).divide(BigDecimal.TWO, 4, RoundingMode.HALF_UP);
    }

    /** Lo que suman por mes: el último importe de cada una, pesos primero. */
    private static List<CurrencyTotal> totals(List<CardSubscription> items) {
        return items.stream().map(CardSubscription::currency).distinct()
                .sorted(Comparator.comparing((String c) -> !"ARS".equals(c)).thenComparing(c -> c))
                .map(c -> {
                    List<CardSubscription> in = items.stream().filter(s -> c.equals(s.currency())).toList();
                    return new CurrencyTotal(c, in.stream().map(CardSubscription::lastAmount).reduce(BigDecimal.ZERO, BigDecimal::add), in.size());
                }).toList();
    }

    private static String required(String merchant) {
        if (merchant == null || merchant.isBlank()) {
            throw new ServiceException("Falta el comercio");
        }
        return normalize(merchant);
    }
}
