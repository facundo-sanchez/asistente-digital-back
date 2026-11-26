package org.acme.card.application.usecase;

import org.acme.card.application.port.output.CardTransactionRepository;
import org.acme.card.domain.model.CardTransaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Movimientos en memoria. */
public class InMemoryCardTransactionRepository implements CardTransactionRepository {

    final List<CardTransaction> rows = new ArrayList<>();

    @Override
    public void saveAll(List<CardTransaction> transactions) {
        transactions.forEach(t -> rows.add(t.toBuilder().id(UUID.randomUUID()).build()));
    }

    @Override
    public List<CardTransaction> findForStatement(UUID userId, UUID statementId) {
        return rows.stream().filter(t -> t.getUserId().equals(userId) && t.getStatementId().equals(statementId))
                .sorted(Comparator.comparing(CardTransaction::getTransactionDate)).toList();
    }
}
