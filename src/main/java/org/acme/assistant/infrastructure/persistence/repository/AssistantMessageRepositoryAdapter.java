package org.acme.assistant.infrastructure.persistence.repository;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.assistant.application.port.output.AssistantMessageRepository;
import org.acme.assistant.domain.model.AssistantMessage;
import org.acme.assistant.domain.model.MessageRole;
import org.acme.assistant.infrastructure.persistence.entity.AssistantMessageEntity;
import org.acme.assistant.infrastructure.persistence.mapper.AssistantPersistenceMapper;

import java.time.Instant;
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
    public long countQuestionsSince(UUID userId, Instant since) {
        return repository.getEntityManager().createQuery("""
                        select count(m) from AssistantMessageEntity m, ConversationEntity c
                        where c.id = m.conversationId and c.userId = :userId and m.role = :role and m.createdAt >= :since""", Long.class)
                .setParameter("userId", userId)
                .setParameter("role", MessageRole.USER)
                .setParameter("since", since)
                .getSingleResult();
    }

    @Override
    @Transactional
    public AssistantMessage save(AssistantMessage message) {
        AssistantMessageEntity entity = mapper.toEntity(message);
        repository.persistAndFlush(entity);
        return mapper.toModel(entity);
    }
}
