package org.acme.user.application.model;

import org.acme.user.domain.model.User;

import java.time.Instant;

/**
 * Sesión emitida al iniciar sesión, registrarse o renovar.
 *
 * @param accessToken  JWT de corta duración para llamar a la API
 * @param refreshToken token opaco de un solo uso para renovar la sesión (rotativo)
 */
public record AuthSession(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        User user
) {}
