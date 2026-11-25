package org.acme.assistant.infrastructure.rest.mapper;

import org.acme.assistant.domain.model.AssistantMessage;
import org.acme.assistant.domain.model.Conversation;
import org.acme.assistant.infrastructure.rest.dto.ConversationDetailResponse;
import org.acme.assistant.infrastructure.rest.dto.ConversationResponse;
import org.acme.assistant.infrastructure.rest.dto.MessageResponse;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface AssistantRestMapper {

    ConversationResponse toResponse(Conversation conversation);

    MessageResponse toResponse(AssistantMessage message);

    @Mapping(target = "id", source = "conversation.id")
    @Mapping(target = "title", source = "conversation.title")
    @Mapping(target = "createdAt", source = "conversation.createdAt")
    @Mapping(target = "updatedAt", source = "conversation.updatedAt")
    @Mapping(target = "messages", source = "messages")
    ConversationDetailResponse toDetail(Conversation conversation, List<AssistantMessage> messages);
}
