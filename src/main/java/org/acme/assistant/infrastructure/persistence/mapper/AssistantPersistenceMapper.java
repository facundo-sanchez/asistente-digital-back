package org.acme.assistant.infrastructure.persistence.mapper;

import org.acme.assistant.domain.model.AssistantMessage;
import org.acme.assistant.domain.model.Conversation;
import org.acme.assistant.infrastructure.persistence.entity.AssistantMessageEntity;
import org.acme.assistant.infrastructure.persistence.entity.ConversationEntity;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface AssistantPersistenceMapper {
    Conversation toModel(ConversationEntity entity);
    ConversationEntity toEntity(Conversation model);

    AssistantMessage toModel(AssistantMessageEntity entity);
    AssistantMessageEntity toEntity(AssistantMessage model);
}
