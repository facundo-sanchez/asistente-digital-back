package org.acme.card.infrastructure.persistence.repository;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.card.application.port.output.CardTransactionRepository;
import org.acme.card.domain.model.CardTransaction;
import org.acme.card.infrastructure.persistence.entity.CardTransactionEntity;
import org.acme.card.infrastructure.persistence.mapper.CardPersistenceMapper;

import java.util.List;
import java.util.Optional;
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

    @Override
    public Optional<CardTransaction> findByIdForUser(UUID userId, UUID transactionId) {
        return repository.find("id = ?1 and userId = ?2", transactionId, userId).firstResultOptional().map(mapper::toModel);
    }

    @Override
    @Transactional
    public CardTransaction save(CardTransaction transaction) {
        CardTransactionEntity entity = repository.getEntityManager().merge(mapper.toEntity(transaction));
        repository.flush();
        return mapper.toModel(entity);
    }

    @Override
    public List<CardTransaction> findByMerchant(UUID userId, String merchantName) {
        return repository.list("userId = ?1 and merchantName = ?2", userId, merchantName).stream().map(mapper::toModel).toList();
    }

    @Override
    public List<CardTransaction> findInstallment(UUID userId, UUID cardId, String merchantName, int total, int number) {
        if (merchantName == null) return List.of();
        return repository.list("userId = ?1 and cardId = ?2 and merchantName = ?3 and installmentTotal = ?4 and installmentNumber = ?5",
                Sort.descending("transactionDate"), userId, cardId, merchantName, total, number).stream().map(mapper::toModel).toList();
    }
}
