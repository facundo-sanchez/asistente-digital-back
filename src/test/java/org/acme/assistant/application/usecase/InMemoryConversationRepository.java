package org.acme.assistant.application.usecase;

import org.acme.assistant.application.port.output.ConversationRepository;
import org.acme.assistant.domain.model.Conversation;

import java.util.*;

public class InMemoryConversationRepository implements ConversationRepository {

    private final Map<UUID, Conversation> store = new LinkedHashMap<>();

    @Override
    public List<Conversation> findForUser(UUID userId) {
        return store.values().stream().filter(c -> c.getUserId().equals(userId) && c.getDeletedAt() == null)
                .sorted(Comparator.comparing(Conversation::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    }

    @Override
    public Optional<Conversation> findByIdForUser(UUID userId, UUID id) {
        return Optional.ofNullable(store.get(id)).filter(c -> c.getUserId().equals(userId) && c.getDeletedAt() == null);
    }

    @Override
    public Conversation save(Conversation conversation) {
        Conversation copy = conversation.toBuilder().id(conversation.getId() == null ? UUID.randomUUID() : conversation.getId()).build();
        store.put(copy.getId(), copy);
        return copy.toBuilder().build();
    }
}
