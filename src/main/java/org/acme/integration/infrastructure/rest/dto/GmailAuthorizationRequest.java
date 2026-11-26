package org.acme.integration.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** state (CSRF) y code challenge PKCE generados por el BFF. */
@Schema(name = "GmailAuthorizationRequest")
public record GmailAuthorizationRequest(
        @NotBlank String state,
        @NotBlank String codeChallenge
) {}
