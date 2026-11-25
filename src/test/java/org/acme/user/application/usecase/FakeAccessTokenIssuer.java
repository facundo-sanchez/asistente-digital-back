package org.acme.user.application.usecase;

import org.acme.shared.application.AppSettings;
import org.acme.user.application.port.output.AccessTokenIssuer;
import org.acme.user.application.port.output.IssuedToken;
import org.acme.user.domain.model.User;

import java.time.Instant;

/** Emite tokens de texto únicos en lugar de JWT firmados. */
public class FakeAccessTokenIssuer implements AccessTokenIssuer {

    private final AppSettings settings;
    private int counter;

    public FakeAccessTokenIssuer(AppSettings settings) {
        this.settings = settings;
    }

    @Override
    public IssuedToken issue(User user, Instant now) {
        counter++;
        return new IssuedToken("access-" + user.getId() + "-" + counter, now.plus(settings.auth().accessTokenTtl()));
    }
}
