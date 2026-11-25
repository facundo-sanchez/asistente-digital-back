package org.acme.user.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** POST /auth/refresh y POST /auth/logout. */
@Schema(name = "RefreshTokenRequest")
public record RefreshTokenRequest(
        @NotBlank(message = "El refresh token es obligatorio")
        @Size(max = 200)
        String refreshToken
) {}
