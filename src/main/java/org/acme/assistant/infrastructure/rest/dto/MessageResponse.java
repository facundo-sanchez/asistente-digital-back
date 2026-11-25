package org.acme.assistant.infrastructure.rest.dto;

import org.acme.assistant.domain.model.MessageRole;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(name = "MessageResponse")
public record MessageResponse(
        UUID id,
        MessageRole role,
        @Schema(example = "Este mes tenés que pagar $ 152.300 en 4 obligaciones.") String content,
        @Schema(description = "Referencias a registros y herramientas usadas") Map<String, Object> metadata,
        Instant createdAt
) {}
