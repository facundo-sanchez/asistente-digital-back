package org.acme.card.application.usecase;

import org.acme.card.application.port.output.CardStatementRepository;
import org.acme.card.domain.model.CardStatement;
import org.acme.card.domain.model.CardStatementStatus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Resúmenes en memoria. Guarda copias. */
public class InMemoryCardStatementRepository implements CardStatementRepository {

    final Map<UUID, CardStatement> rows = new LinkedHashMap<>();

    @Override
    public CardStatement save(CardStatement statement) {
        CardStatement copy = statement.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
        }
        rows.put(copy.getId(), copy);
        return copy.toBuilder().build();
    }

    @Override
    public Optional<CardStatement> findByIdForUser(UUID userId, UUID statementId) {
        return Optional.ofNullable(rows.get(statementId)).filter(s -> s.getUserId().equals(userId)).map(s -> s.toBuilder().build());
    }

    @Override
    public List<CardStatement> findForCard(UUID userId, UUID cardId) {
        return rows.values().stream()
                .filter(s -> s.getUserId().equals(userId) && s.getCardId().equals(cardId))
                .sorted(Comparator.comparing(CardStatement::getDueDate).reversed())
                .map(s -> s.toBuilder().build())
                .toList();
    }

    @Override
    public Optional<CardStatement> findConfirmedByDueDate(UUID userId, UUID cardId, LocalDate dueDate) {
        return rows.values().stream()
                .filter(s -> s.getUserId().equals(userId) && s.getCardId().equals(cardId) && s.getDueDate().equals(dueDate)
                        && s.getStatus() == CardStatementStatus.CONFIRMED)
                .findFirst();
    }

    @Override
    public Optional<CardStatement> findByObligation(UUID userId, UUID obligationId) {
        return rows.values().stream()
                .filter(s -> s.getUserId().equals(userId) && (obligationId.equals(s.getObligationId()) || obligationId.equals(s.getObligationUsdId())))
                .findFirst();
    }

    @Override
    public Optional<CardStatement> findByExtraction(UUID userId, UUID extractionResultId) {
        return rows.values().stream().filter(s -> s.getUserId().equals(userId) && extractionResultId.equals(s.getExtractionResultId())).findFirst();
    }
}
