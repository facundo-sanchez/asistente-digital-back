package org.acme.assistant.infrastructure.persistence.repository;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.assistant.application.port.output.ConversationRepository;
import org.acme.assistant.domain.model.Conversation;
import org.acme.assistant.infrastructure.persistence.entity.ConversationEntity;
import org.acme.assistant.infrastructure.persistence.mapper.AssistantPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ConversationRepositoryAdapter implements ConversationRepository {

    @Inject
    ConversationPanacheRepository repository;

    @Inject
    AssistantPersistenceMapper mapper;

    @Override
    public List<Conversation> findForUser(UUID userId) {
        return repository.list("userId = ?1 and deletedAt is null", Sort.descending("updatedAt"), userId).stream().map(mapper::toModel).toList();
    }

    @Override
    public Optional<Conversation> findByIdForUser(UUID userId, UUID id) {
        return repository.find("id = ?1 and userId = ?2 and deletedAt is null", id, userId).firstResultOptional().map(mapper::toModel);
    }

    @Override
    @Transactional
    public Conversation save(Conversation conversation) {
        ConversationEntity entity = mapper.toEntity(conversation);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }
}
