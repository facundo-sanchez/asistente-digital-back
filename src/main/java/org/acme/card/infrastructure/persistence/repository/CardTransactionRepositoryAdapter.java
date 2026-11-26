package org.acme.card.infrastructure.persistence.repository;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.card.application.port.output.CardTransactionRepository;
import org.acme.card.domain.model.CardTransaction;
import org.acme.card.infrastructure.persistence.mapper.CardPersistenceMapper;

import java.util.List;
import java.util.UUID;

/** Adaptador de persistencia del puerto {@link CardTransactionRepository}. */
@ApplicationScoped
public class CardTransactionRepositoryAdapter implements CardTransactionRepository {

    @Inject
    CardTransactionPanacheRepository repository;

    @Inject
    CardPersistenceMapper mapper;

    @Override
    public void saveAll(List<CardTransaction> transactions) {
        repository.persist(transactions.stream().map(mapper::toEntity));
        repository.flush();
    }

    @Override
    public List<CardTransaction> findForStatement(UUID userId, UUID statementId) {
        return repository.list("userId = ?1 and statementId = ?2", Sort.by("transactionDate"), userId, statementId)
                .stream().map(mapper::toModel).toList();
    }
}
