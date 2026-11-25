package org.acme.user.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "GoogleAuthorizationResponse")
public record GoogleAuthorizationResponse(
        @Schema(description = "URL de Google a la que redirigir al usuario")
        String url
) {}
