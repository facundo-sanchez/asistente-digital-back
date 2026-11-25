package org.acme.user.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** POST /auth/verify-email: token recibido por email. */
@Schema(name = "TokenRequest")
public record TokenRequest(
        @NotBlank(message = "El token es obligatorio")
        @Size(max = 200)
        String token
) {}
