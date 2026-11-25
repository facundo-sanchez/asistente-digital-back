package org.acme.user.application.usecase;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.ConflictException;
import org.acme.domain.exceptions.NotAuthorizedException;
import org.acme.user.application.model.AuthSession;
import org.acme.user.application.port.output.PasswordHasher;
import org.acme.user.application.port.output.UserRepository;
import org.acme.user.application.port.output.UserTokenRepository;
import org.acme.user.application.security.OpaqueTokens;
import org.acme.user.domain.model.User;
import org.acme.user.domain.model.UserToken;
import org.acme.user.domain.model.UserTokenType;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Inicio de sesión, renovación (refresh rotativo) y cierre de sesión. */
@ApplicationScoped
public class AuthenticationUseCase {

    static final String INVALID_CREDENTIALS = "Email o contraseña incorrectos";
    static final String INVALID_SESSION = "La sesión no es válida o expiró";
    static final String ALREADY_ROTATED = "La sesión ya se renovó en otro pedido";

    /**
     * Si un refresh token ya usado vuelve a presentarse dentro de esta ventana se asume
     * una carrera benigna (dos pestañas renovando a la vez) y se responde 409. Fuera de
     * la ventana se trata como robo del token: se cierran todas las sesiones del usuario.
     */
    static final Duration REUSE_GRACE = Duration.ofSeconds(30);

    private final UserRepository users;
    private final UserTokenRepository tokens;
    private final PasswordHasher passwordHasher;
    private final SessionIssuer sessionIssuer;
    private final Clock clock;

    /** Hash de referencia para igualar el tiempo de respuesta cuando el email no existe. */
    private volatile String dummyHash;

    @Inject
    public AuthenticationUseCase(UserRepository users, UserTokenRepository tokens, PasswordHasher passwordHasher,
                                 SessionIssuer sessionIssuer, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.passwordHasher = passwordHasher;
        this.sessionIssuer = sessionIssuer;
        this.clock = clock;
    }

    @Transactional
    public AuthSession login(String email, String password) {
        Optional<User> found = users.findByEmail(User.normalizeEmail(email));

        if (found.isEmpty() || !found.get().hasPassword()) {
            // Mismo costo que una verificación real: no revela si el email existe
            passwordHasher.matches(password, dummyHash());
            throw new NotAuthorizedException(INVALID_CREDENTIALS);
        }

        User user = found.get();
        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            throw new NotAuthorizedException(INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new NotAuthorizedException("La cuenta está deshabilitada");
        }

        user.setLastLoginAt(clock.instant());
        users.save(user);
        return sessionIssuer.issue(user);
    }

    /**
     * dontRollbackOn: si se detecta reutilización, la invalidación de todas las sesiones
     * debe persistir aunque la llamada termine en 401.
     *
     * @throws ConflictException si otro pedido acaba de renovar con este mismo token (dos
     *         pestañas o llamadas en paralelo): la sesión sigue viva con las cookies nuevas,
     *         el cliente no debe borrarlas.
     */
    @Transactional(dontRollbackOn = NotAuthorizedException.class)
    public AuthSession refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        UserToken token = tokens.findByHash(OpaqueTokens.hash(rawRefreshToken))
                .filter(t -> t.getTokenType() == UserTokenType.REFRESH)
                .orElseThrow(() -> new NotAuthorizedException(INVALID_SESSION));

        if (token.getUsedAt() != null) {
            rejectReuse(token, now);
        }
        if (!token.getExpiresAt().isAfter(now)) {
            throw new NotAuthorizedException(INVALID_SESSION);
        }
        // Atómico: si dos pedidos llegan juntos, solo uno lo marca
        if (!tokens.markUsed(token.getId(), now)) {
            throw new ConflictException(ALREADY_ROTATED);
        }

        User user = users.findById(token.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new NotAuthorizedException(INVALID_SESSION));
        return sessionIssuer.issue(user);
    }

    /** Invalida el refresh token. Idempotente: no informa si el token existía. */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        Instant now = clock.instant();
        tokens.findByHash(OpaqueTokens.hash(rawRefreshToken))
                .filter(t -> t.getTokenType() == UserTokenType.REFRESH && t.getUsedAt() == null)
                .ifPresent(t -> {
                    t.revoke(now);
                    tokens.save(t);
                });
    }

    /**
     * Token ya usado. Revocado (logout, cambio de contraseña): 401. Rotado hace menos de
     * REUSE_GRACE: carrera benigna, 409. Rotado antes: posible robo, se cierran todas las sesiones.
     */
    private void rejectReuse(UserToken token, Instant now) {
        if (token.wasRevoked()) {
            throw new NotAuthorizedException(INVALID_SESSION);
        }
        if (!token.getUsedAt().plus(REUSE_GRACE).isBefore(now)) {
            throw new ConflictException(ALREADY_ROTATED);
        }
        Log.warnf("Reutilización de refresh token del usuario %s: se cierran todas sus sesiones", token.getUserId());
        tokens.invalidateAll(token.getUserId(), UserTokenType.REFRESH, now);
        throw new NotAuthorizedException(INVALID_SESSION);
    }

    private String dummyHash() {
        String hash = dummyHash;
        if (hash == null) {
            hash = passwordHasher.hash(OpaqueTokens.generate());
            dummyHash = hash;
        }
        return hash;
    }
}
