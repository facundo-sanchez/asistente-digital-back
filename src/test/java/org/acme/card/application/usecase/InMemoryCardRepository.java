package org.acme.card.application.usecase;

import org.acme.card.application.port.output.CardRepository;
import org.acme.card.domain.model.Card;
import org.acme.card.domain.model.CardBrand;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Tarjetas en memoria. Guarda copias. */
public class InMemoryCardRepository implements CardRepository {

    public final Map<UUID, Card> rows = new LinkedHashMap<>();

    @Override
    public Card save(Card card) {
        Card copy = card.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
        }
        rows.put(copy.getId(), copy);
        return copy.toBuilder().build();
    }

    @Override
    public Optional<Card> findByIdForUser(UUID userId, UUID cardId) {
        return Optional.ofNullable(rows.get(cardId)).filter(c -> c.getUserId().equals(userId)).map(c -> c.toBuilder().build());
    }

    @Override
    public List<Card> findForUser(UUID userId, Boolean active) {
        return rows.values().stream()
                .filter(c -> c.getUserId().equals(userId) && (active == null || c.isActive() == active))
                .sorted(Comparator.comparing(Card::getName))
                .map(c -> c.toBuilder().build())
                .toList();
    }

    @Override
    public Optional<Card> findByBrandAndLastFour(UUID userId, CardBrand brand, String lastFour) {
        return rows.values().stream()
                .filter(c -> c.getUserId().equals(userId) && c.getBrand() == brand && lastFour.equals(c.getLastFour()))
                .findFirst();
    }
}
