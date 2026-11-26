package org.acme.card.infrastructure.persistence.repository;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.card.application.port.output.CardStatementRepository;
import org.acme.card.domain.model.CardStatement;
import org.acme.card.domain.model.CardStatementStatus;
import org.acme.card.infrastructure.persistence.entity.CardStatementEntity;
import org.acme.card.infrastructure.persistence.mapper.CardPersistenceMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de persistencia del puerto {@link CardStatementRepository}. */
@ApplicationScoped
public class CardStatementRepositoryAdapter implements CardStatementRepository {

    @Inject
    CardStatementPanacheRepository repository;

    @Inject
    CardPersistenceMapper mapper;

    @Override
    public CardStatement save(CardStatement statement) {
        CardStatementEntity entity = mapper.toEntity(statement);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    public Optional<CardStatement> findByIdForUser(UUID userId, UUID statementId) {
        return repository.find("id = ?1 and userId = ?2", statementId, userId).firstResultOptional().map(mapper::toModel);
    }

    @Override
    public List<CardStatement> findForCard(UUID userId, UUID cardId) {
        return repository.list("userId = ?1 and cardId = ?2", Sort.descending("dueDate"), userId, cardId)
                .stream().map(mapper::toModel).toList();
    }

    @Override
    public Optional<CardStatement> findConfirmedByDueDate(UUID userId, UUID cardId, LocalDate dueDate) {
        return repository.find("userId = ?1 and cardId = ?2 and dueDate = ?3 and status = ?4",
                        userId, cardId, dueDate, CardStatementStatus.CONFIRMED)
                .firstResultOptional()
                .map(mapper::toModel);
    }

    @Override
    public Optional<CardStatement> findByObligation(UUID userId, UUID obligationId) {
        return repository.find("userId = ?1 and (obligationId = ?2 or obligationUsdId = ?2)", userId, obligationId)
                .firstResultOptional()
                .map(mapper::toModel);
    }
}
