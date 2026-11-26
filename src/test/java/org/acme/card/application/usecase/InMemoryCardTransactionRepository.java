package org.acme.card.application.usecase;

import org.acme.card.application.port.output.CardTransactionRepository;
import org.acme.card.domain.model.CardTransaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    @Override
    public Optional<CardTransaction> findByIdForUser(UUID userId, UUID transactionId) {
        return rows.stream().filter(t -> t.getId().equals(transactionId) && t.getUserId().equals(userId)).findFirst().map(t -> t.toBuilder().build());
    }

    @Override
    public CardTransaction save(CardTransaction transaction) {
        rows.replaceAll(t -> t.getId().equals(transaction.getId()) ? transaction.toBuilder().build() : t);
        return transaction;
    }

    @Override
    public List<CardTransaction> findByMerchant(UUID userId, String merchantName) {
        return rows.stream().filter(t -> t.getUserId().equals(userId) && Objects.equals(t.getMerchantName(), merchantName))
                .map(t -> t.toBuilder().build()).toList();
    }

    @Override
    public List<CardTransaction> findInstallment(UUID userId, UUID cardId, String merchantName, int total, int number) {
        return rows.stream().filter(t -> t.getUserId().equals(userId) && t.getCardId().equals(cardId) && Objects.equals(t.getMerchantName(), merchantName)
                        && Objects.equals(t.getInstallmentTotal(), total) && Objects.equals(t.getInstallmentNumber(), number))
                .map(t -> t.toBuilder().build()).toList();
    }
}
