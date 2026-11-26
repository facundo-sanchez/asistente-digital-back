package org.acme.assistant.infrastructure.persistence.repository;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.assistant.application.port.output.AssistantMessageRepository;
import org.acme.assistant.domain.model.AssistantMessage;
import org.acme.assistant.infrastructure.persistence.entity.AssistantMessageEntity;
import org.acme.assistant.infrastructure.persistence.mapper.AssistantPersistenceMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AssistantMessageRepositoryAdapter implements AssistantMessageRepository {

    @Inject
    AssistantMessagePanacheRepository repository;

    @Inject
    AssistantPersistenceMapper mapper;

    @Override
    public List<AssistantMessage> findByConversation(UUID conversationId) {
        return repository.list("conversationId", Sort.ascending("createdAt"), conversationId).stream().map(mapper::toModel).toList();
    }

    @Override
    public List<AssistantMessage> findLast(UUID conversationId, int limit) {
        List<AssistantMessage> last = new ArrayList<>(repository.find("conversationId", Sort.descending("createdAt"), conversationId)
                .page(Page.ofSize(limit)).list().stream().map(mapper::toModel).toList());
        Collections.reverse(last);
        return last;
    }

    @Override
    @Transactional
    public AssistantMessage save(AssistantMessage message) {
        AssistantMessageEntity entity = mapper.toEntity(message);
        repository.persistAndFlush(entity);
        return mapper.toModel(entity);
    }
}
