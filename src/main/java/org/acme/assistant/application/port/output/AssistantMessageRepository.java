package org.acme.assistant.application.port.output;

import org.acme.assistant.domain.model.AssistantMessage;

import java.util.List;
import java.util.UUID;

public interface AssistantMessageRepository {

    /** Todos, los más viejos primero. */
    List<AssistantMessage> findByConversation(UUID conversationId);

    /** Los últimos limit, los más viejos primero. */
    List<AssistantMessage> findLast(UUID conversationId, int limit);

    AssistantMessage save(AssistantMessage message);
}
