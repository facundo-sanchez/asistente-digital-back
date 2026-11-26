package org.acme.card.infrastructure.persistence.repository;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.card.application.port.output.CardRepository;
import org.acme.card.domain.model.Card;
import org.acme.card.domain.model.CardBrand;
import org.acme.card.infrastructure.persistence.entity.CardEntity;
import org.acme.card.infrastructure.persistence.mapper.CardPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de persistencia del puerto {@link CardRepository}. */
@ApplicationScoped
public class CardRepositoryAdapter implements CardRepository {

    @Inject
    CardPanacheRepository repository;

    @Inject
    CardPersistenceMapper mapper;

    @Override
    public Card save(Card card) {
        CardEntity entity = mapper.toEntity(card);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    public Optional<Card> findByIdForUser(UUID userId, UUID cardId) {
        return repository.find("id = ?1 and userId = ?2", cardId, userId).firstResultOptional().map(mapper::toModel);
    }

    @Override
    public List<Card> findForUser(UUID userId, Boolean active) {
        List<CardEntity> entities = active == null
                ? repository.list("userId", Sort.by("name"), userId)
                : repository.list("userId = ?1 and active = ?2", Sort.by("name"), userId, active);
        return entities.stream().map(mapper::toModel).toList();
    }

    @Override
    public Optional<Card> findByBrandAndLastFour(UUID userId, CardBrand brand, String lastFour) {
        return repository.find("userId = ?1 and brand = ?2 and lastFour = ?3", userId, brand, lastFour)
                .firstResultOptional()
                .map(mapper::toModel);
    }
}
