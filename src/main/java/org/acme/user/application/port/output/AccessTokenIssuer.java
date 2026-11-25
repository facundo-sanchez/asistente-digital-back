package org.acme.user.application.port.output;

import org.acme.user.domain.model.User;

import java.time.Instant;

/** Emite el access token (JWT firmado) de un usuario. */
public interface AccessTokenIssuer {

    IssuedToken issue(User user, Instant now);
}
