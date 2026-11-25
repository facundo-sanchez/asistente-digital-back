package org.acme.user.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.NotAuthorizedException;
import org.acme.domain.exceptions.UnavailableException;
import org.acme.shared.application.AppSettings;
import org.acme.user.application.model.AuthSession;
import org.acme.user.application.model.GoogleIdentity;
import org.acme.user.application.port.output.GoogleIdentityProvider;
import org.acme.user.application.port.output.UserRepository;
import org.acme.user.application.port.output.UserTokenRepository;
import org.acme.user.domain.model.User;
import org.acme.user.domain.model.UserTokenType;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;

/**
 * Inicio de sesión con Google. Busca al usuario por su cuenta de Google; si no la tiene
 * vinculada, la vincula a la cuenta con el mismo email o crea una cuenta nueva.
 */
@ApplicationScoped
public class GoogleLoginUseCase {

    /** Ruta del BFF que recibe la respuesta de Google. Debe estar registrada en Google Cloud. */
    public static final String CALLBACK_PATH = "/api/auth/google/callback";

    static final String NOT_CONFIGURED = "El inicio de sesión con Google no está disponible";
    static final String UNVERIFIED_EMAIL = "Tu cuenta de Google no tiene el email verificado";
    static final String OTHER_GOOGLE_ACCOUNT = "Tu cuenta ya está vinculada a otra cuenta de Google";
    static final String DISABLED = "La cuenta está deshabilitada";

    private static final int MAX_NAME_LENGTH = 150;

    private final GoogleIdentityProvider google;
    private final UserRepository users;
    private final UserTokenRepository tokens;
    private final SessionIssuer sessionIssuer;
    private final AppSettings settings;
    private final Clock clock;

    @Inject
    public GoogleLoginUseCase(GoogleIdentityProvider google, UserRepository users, UserTokenRepository tokens,
                              SessionIssuer sessionIssuer, AppSettings settings, Clock clock) {
        this.google = google;
        this.users = users;
        this.tokens = tokens;
        this.sessionIssuer = sessionIssuer;
        this.settings = settings;
        this.clock = clock;
    }

    /** URL de Google para elegir la cuenta. state y codeChallenge los genera el BFF. */
    public URI authorizationUrl(String state, String codeChallenge) {
        ensureConfigured();
        return google.authorizationUrl(state, codeChallenge, redirectUri());
    }

    @Transactional
    public AuthSession login(String code, String codeVerifier) {
        ensureConfigured();
        GoogleIdentity identity = google.exchangeCode(code, codeVerifier, redirectUri());

        User user = users.findByGoogleSubject(identity.subject())
                .orElseGet(() -> linkOrCreate(identity));
        if (!user.isActive()) {
            throw new NotAuthorizedException(DISABLED);
        }

        user.setLastLoginAt(clock.instant());
        return sessionIssuer.issue(users.save(user));
    }

    private User linkOrCreate(GoogleIdentity identity) {
        // Sin email verificado no se puede asegurar que la cuenta de Google sea del dueño del email
        if (!identity.emailVerified()) {
            throw new NotAuthorizedException(UNVERIFIED_EMAIL);
        }
        String email = User.normalizeEmail(identity.email());
        return users.findByEmail(email)
                .map(existing -> link(existing, identity))
                .orElseGet(() -> create(email, identity));
    }

    /**
     * Vincula Google a la cuenta que ya tiene ese email. Si la cuenta nunca verificó el email,
     * sus datos pudo haberlos cargado otra persona (alguien que registró un email ajeno antes
     * que su dueño): se descartan la contraseña y el nombre, y se cierran sus sesiones. El
     * dueño puede crear una contraseña nueva desde su perfil.
     */
    private User link(User user, GoogleIdentity identity) {
        if (user.getGoogleSubject() != null) {
            throw new NotAuthorizedException(OTHER_GOOGLE_ACCOUNT);
        }
        Instant now = clock.instant();
        if (!user.isEmailVerified()) {
            user.setPasswordHash(null);
            user.setName(displayName(identity, user.getEmail()));
            tokens.invalidateAll(user.getId(), UserTokenType.REFRESH, now);
        }
        user.setGoogleSubject(identity.subject());
        user.verifyEmail(now);
        return user;
    }

    /** Cuenta nueva sin contraseña. El email ya viene verificado por Google. */
    private User create(String email, GoogleIdentity identity) {
        return User.builder()
                .email(email)
                .name(displayName(identity, email))
                .googleSubject(identity.subject())
                .emailVerifiedAt(clock.instant())
                .build();
    }

    private static String displayName(GoogleIdentity identity, String email) {
        String name = identity.name() == null || identity.name().isBlank()
                ? email.substring(0, email.indexOf('@'))
                : identity.name().trim();
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }

    private void ensureConfigured() {
        if (settings.google().clientId().isEmpty() || settings.google().clientSecret().isEmpty()) {
            throw new UnavailableException(NOT_CONFIGURED);
        }
    }

    private String redirectUri() {
        return settings.frontendUrl().replaceAll("/+$", "") + CALLBACK_PATH;
    }
}
