package org.acme.user.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.shared.application.AppSettings;
import org.acme.user.application.model.AuthSession;
import org.acme.user.application.port.output.AccessTokenIssuer;
import org.acme.user.application.port.output.IssuedToken;
import org.acme.user.application.port.output.UserTokenRepository;
import org.acme.user.application.security.OpaqueTokens;
import org.acme.user.domain.model.User;
import org.acme.user.domain.model.UserToken;
import org.acme.user.domain.model.UserTokenType;

import java.time.Clock;
import java.time.Instant;

/** Emite una sesión: access token JWT + refresh token opaco (se persiste solo su hash). */
@ApplicationScoped
public class SessionIssuer {

    private final AccessTokenIssuer accessTokenIssuer;
    private final UserTokenRepository tokens;
    private final AppSettings settings;
    private final Clock clock;

    @Inject
    public SessionIssuer(AccessTokenIssuer accessTokenIssuer, UserTokenRepository tokens, AppSettings settings, Clock clock) {
        this.accessTokenIssuer = accessTokenIssuer;
        this.tokens = tokens;
        this.settings = settings;
        this.clock = clock;
    }

    public AuthSession issue(User user) {
        Instant now = clock.instant();
        IssuedToken access = accessTokenIssuer.issue(user, now);

        String refresh = OpaqueTokens.generate();
        Instant refreshExpiresAt = now.plus(settings.auth().refreshTokenTtl());
        tokens.save(UserToken.builder()
                .userId(user.getId())
                .tokenType(UserTokenType.REFRESH)
                .tokenHash(OpaqueTokens.hash(refresh))
                .expiresAt(refreshExpiresAt)
                .build());

        return new AuthSession(access.token(), access.expiresAt(), refresh, refreshExpiresAt, user);
    }
}
