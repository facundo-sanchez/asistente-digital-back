package org.acme.assistant.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(name = "ConversationDetailResponse")
public record ConversationDetailResponse(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<MessageResponse> messages
) {}
