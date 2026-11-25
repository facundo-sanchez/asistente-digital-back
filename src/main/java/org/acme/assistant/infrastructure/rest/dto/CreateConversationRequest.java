package org.acme.assistant.infrastructure.rest.dto;

import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CreateConversationRequest")
public record CreateConversationRequest(
        @Size(max = 200)
        @Schema(description = "Opcional; si falta se genera a partir del primer mensaje") String title
) {}
