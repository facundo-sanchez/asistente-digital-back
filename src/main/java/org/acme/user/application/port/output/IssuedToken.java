package org.acme.user.application.port.output;

import java.time.Instant;

/** Access token emitido y su vencimiento. */
public record IssuedToken(String token, Instant expiresAt) {}
