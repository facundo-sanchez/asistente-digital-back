package org.acme.assistant.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "SendMessageRequest")
public record SendMessageRequest(
        @NotBlank(message = "La consulta no puede estar vacía")
        @Size(max = 4000)
        @Schema(example = "¿Cuánto tengo que pagar este mes?") String content
) {}
