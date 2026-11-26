package org.acme.assistant.application.port.output;

import org.acme.assistant.domain.model.Conversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository {

    /** Las no borradas, la de actividad más reciente primero. */
    List<Conversation> findForUser(UUID userId);

    /** Solo si es del usuario y no está borrada. */
    Optional<Conversation> findByIdForUser(UUID userId, UUID id);

    /** También actualiza updatedAt, para ordenar la lista. */
    Conversation save(Conversation conversation);
}
