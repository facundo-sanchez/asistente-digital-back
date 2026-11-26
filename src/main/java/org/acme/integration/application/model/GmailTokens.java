package org.acme.integration.application.model;

import java.time.Instant;

/** Lo que devuelve Google al canjear el código o renovar. refreshToken solo viene al conectar. */
public record GmailTokens(String accessToken, String refreshToken, Instant expiresAt) {
}
