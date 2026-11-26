package org.acme.assistant.application.usecase;

import org.acme.assistant.application.port.output.AssistantMessageRepository;
import org.acme.assistant.domain.model.AssistantMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryAssistantMessageRepository implements AssistantMessageRepository {

    private final List<AssistantMessage> store = new ArrayList<>();

    @Override
    public List<AssistantMessage> findByConversation(UUID conversationId) {
        return store.stream().filter(m -> m.getConversationId().equals(conversationId)).toList();
    }

    @Override
    public List<AssistantMessage> findLast(UUID conversationId, int limit) {
        List<AssistantMessage> all = findByConversation(conversationId);
        return all.subList(Math.max(0, all.size() - limit), all.size());
    }

    @Override
    public AssistantMessage save(AssistantMessage message) {
        AssistantMessage copy = message.toBuilder().id(UUID.randomUUID()).build();
        store.add(copy);
        return copy;
    }
}
