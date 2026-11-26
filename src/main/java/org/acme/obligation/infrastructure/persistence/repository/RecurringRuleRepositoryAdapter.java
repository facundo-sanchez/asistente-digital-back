package org.acme.obligation.infrastructure.persistence.repository;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.obligation.application.port.output.RecurringRuleRepository;
import org.acme.obligation.domain.model.RecurringRule;
import org.acme.obligation.infrastructure.persistence.entity.RecurringRuleEntity;
import org.acme.obligation.infrastructure.persistence.mapper.ObligationPersistenceMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de persistencia del puerto {@link RecurringRuleRepository}. */
@ApplicationScoped
public class RecurringRuleRepositoryAdapter implements RecurringRuleRepository {

    @Inject
    RecurringRulePanacheRepository repository;

    @Inject
    ObligationPersistenceMapper mapper;

    @Override
    public RecurringRule save(RecurringRule rule) {
        RecurringRuleEntity entity = mapper.toEntity(rule);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    public Optional<RecurringRule> findByIdForUser(UUID userId, UUID ruleId) {
        return repository.find("id = ?1 and userId = ?2", ruleId, userId).firstResultOptional().map(mapper::toModel);
    }

    @Override
    public List<RecurringRule> findForUser(UUID userId) {
        return repository.list("userId", Sort.by("title"), userId).stream().map(mapper::toModel).toList();
    }

    @Override
    public List<RecurringRule> findActiveDueUntil(LocalDate limit) {
        return repository.list("active = true and nextExpectedDate <= ?1", limit).stream().map(mapper::toModel).toList();
    }
}
