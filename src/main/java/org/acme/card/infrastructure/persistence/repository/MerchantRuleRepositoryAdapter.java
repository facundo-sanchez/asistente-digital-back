package org.acme.card.infrastructure.persistence.repository;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.card.application.port.output.MerchantRuleRepository;
import org.acme.card.domain.model.CardMerchantRule;
import org.acme.card.infrastructure.persistence.entity.CardMerchantRuleEntity;
import org.acme.card.infrastructure.persistence.mapper.CardPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MerchantRuleRepositoryAdapter implements MerchantRuleRepository {

    @Inject
    CardMerchantRulePanacheRepository repository;

    @Inject
    CardPersistenceMapper mapper;

    @Override
    public List<CardMerchantRule> findForUser(UUID userId) {
        return repository.list("userId", Sort.by("merchantName"), userId).stream().map(mapper::toModel).toList();
    }

    @Override
    public Optional<CardMerchantRule> findByMerchant(UUID userId, String merchantName) {
        return repository.find("userId = ?1 and merchantName = ?2", userId, merchantName).firstResultOptional().map(mapper::toModel);
    }

    @Override
    @Transactional
    public CardMerchantRule save(CardMerchantRule rule) {
        CardMerchantRuleEntity entity = mapper.toEntity(rule);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    @Transactional
    public boolean delete(UUID userId, UUID ruleId) {
        return repository.delete("id = ?1 and userId = ?2", ruleId, userId) > 0;
    }
}
