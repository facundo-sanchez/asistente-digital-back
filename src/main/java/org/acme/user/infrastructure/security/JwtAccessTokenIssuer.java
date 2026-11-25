package org.acme.user.infrastructure.security;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.shared.application.AppSettings;
import org.acme.user.application.port.output.AccessTokenIssuer;
import org.acme.user.application.port.output.IssuedToken;
import org.acme.user.domain.model.User;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Access token JWT RS256 firmado con la clave privada (smallrye.jwt.sign.key.location).
 * sub = id del usuario; groups = rol (para @RolesAllowed).
 */
@ApplicationScoped
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    @Inject
    AppSettings settings;

    @Override
    public IssuedToken issue(User user, Instant now) {
        Instant expiresAt = now.plus(settings.auth().accessTokenTtl());
        String token = Jwt.issuer(settings.auth().issuer())
                .audience(settings.auth().audience())
                .subject(user.getId().toString())
                .upn(user.getEmail())
                .groups(Set.of(user.getRole().name()))
                .claim("jti", UUID.randomUUID().toString())
                .claim("name", user.getName())
                .claim("email_verified", user.isEmailVerified())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .sign();
        return new IssuedToken(token, expiresAt);
    }
}
