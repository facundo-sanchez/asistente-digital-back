package org.acme.user.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Sesión emitida. El BFF de Next.js guarda los tokens en cookies httpOnly;
 * el navegador nunca los recibe.
 */
@Schema(name = "AuthResponse")
public record AuthResponse(
        String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Segundos de vida del access token", example = "900") long expiresIn,
        String refreshToken,
        @Schema(description = "Segundos de vida del refresh token", example = "2592000") long refreshExpiresIn,
        UserResponse user
) {}
