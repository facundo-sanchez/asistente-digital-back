package org.acme.user.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** POST /auth/google: código que Google devolvió al callback del BFF. */
@Schema(name = "GoogleLoginRequest")
public record GoogleLoginRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 2048)
        String code,

        @NotBlank(message = "El code verifier es obligatorio")
        @Size(min = 43, max = 128)
        @Pattern(regexp = "^[A-Za-z0-9._~-]+$", message = "El code verifier tiene caracteres inválidos")
        @Schema(description = "PKCE: valor original del code challenge")
        String codeVerifier
) {}
