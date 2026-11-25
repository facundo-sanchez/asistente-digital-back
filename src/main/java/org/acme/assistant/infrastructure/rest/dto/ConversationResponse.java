package org.acme.assistant.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "ConversationResponse")
public record ConversationResponse(
        UUID id,
        @Schema(example = "Gastos de diciembre") String title,
        Instant createdAt,
        Instant updatedAt
) {}
