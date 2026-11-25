package org.acme.user.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** POST /auth/google/authorization: valores que genera el BFF para este intento de login. */
@Schema(name = "GoogleAuthorizationRequest")
public record GoogleAuthorizationRequest(
        @NotBlank(message = "El state es obligatorio")
        @Size(min = 16, max = 128)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "El state debe ser base64url")
        @Schema(description = "Valor aleatorio que Google devuelve en el callback (protección CSRF)")
        String state,

        @NotBlank(message = "El code challenge es obligatorio")
        @Size(min = 43, max = 43, message = "El code challenge debe ser un SHA-256 en base64url")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "El code challenge debe ser base64url")
        @Schema(description = "PKCE: SHA-256 del code verifier, en base64url")
        String codeChallenge
) {}
