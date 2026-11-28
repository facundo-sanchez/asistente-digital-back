package org.acme.card.application.usecase;

import org.acme.card.application.model.SubscriptionCharge;
import org.acme.card.application.port.output.CardSubscriptionRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** Consumos cargados a mano por el test (ya filtrados como los da la base) y los comercios descartados por usuario. */
class InMemoryCardSubscriptionRepository implements CardSubscriptionRepository {

    final Map<UUID, List<SubscriptionCharge>> charges = new HashMap<>();
    private final Map<UUID, Set<String>> dismissed = new HashMap<>();

    @Override
    public List<SubscriptionCharge> charges(UUID userId, UUID cardId, LocalDate since) {
        return charges.getOrDefault(userId, List.of()).stream()
                .filter(c -> !c.closingDate().isBefore(since) && (cardId == null || cardId.equals(c.cardId()))).toList();
    }

    @Override
    public Set<String> dismissed(UUID userId) {
        return Set.copyOf(dismissed.getOrDefault(userId, Set.of()));
    }

    @Override
    public void dismiss(UUID userId, String merchant, Instant now) {
        dismissed.computeIfAbsent(userId, k -> new HashSet<>()).add(merchant);
    }

    @Override
    public void restore(UUID userId, String merchant) {
        dismissed.getOrDefault(userId, new HashSet<>()).remove(merchant);
    }
}
