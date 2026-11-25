package org.acme.integration.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** URL de consentimiento OAuth a la que el frontend redirige al usuario. */
@Schema(name = "AuthorizationUrlResponse")
public record AuthorizationUrlResponse(
        @Schema(example = "https://accounts.google.com/o/oauth2/v2/auth?...") String authorizationUrl
) {}
