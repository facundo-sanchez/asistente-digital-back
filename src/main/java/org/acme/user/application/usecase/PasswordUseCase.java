package org.acme.user.application.usecase;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.shared.application.AppSettings;
import org.acme.shared.application.port.output.EmailSender;
import org.acme.user.application.model.AuthSession;
import org.acme.user.application.port.output.PasswordHasher;
import org.acme.user.application.port.output.UserRepository;
import org.acme.user.application.port.output.UserTokenRepository;
import org.acme.user.application.security.OpaqueTokens;
import org.acme.user.domain.model.User;
import org.acme.user.domain.model.UserToken;
import org.acme.user.domain.model.UserTokenType;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Recupero y cambio de contraseña. */
@ApplicationScoped
public class PasswordUseCase {

    private final UserRepository users;
    private final UserTokenRepository tokens;
    private final PasswordHasher passwordHasher;
    private final EmailSender emailSender;
    private final AuthEmails emails;
    private final SessionIssuer sessionIssuer;
    private final AppSettings settings;
    private final Clock clock;

    @Inject
    public PasswordUseCase(UserRepository users, UserTokenRepository tokens, PasswordHasher passwordHasher,
                           EmailSender emailSender, AuthEmails emails, SessionIssuer sessionIssuer,
                           AppSettings settings, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.passwordHasher = passwordHasher;
        this.emailSender = emailSender;
        this.emails = emails;
        this.sessionIssuer = sessionIssuer;
        this.settings = settings;
        this.clock = clock;
    }

    /**
     * Envía el enlace de reseteo si la cuenta existe. El resultado visible es siempre
     * el mismo, para no revelar qué emails están registrados.
     */
    @Transactional
    public void forgot(String email) {
        User user = users.findByEmail(User.normalizeEmail(email)).filter(User::isActive).orElse(null);
        if (user == null) {
            Log.debug("Recupero de contraseña para un email no registrado");
            return;
        }
        Instant now = clock.instant();
        tokens.invalidateAll(user.getId(), UserTokenType.PASSWORD_RESET, now);
        String token = OpaqueTokens.generate();
        tokens.save(UserToken.builder()
                .userId(user.getId())
                .tokenType(UserTokenType.PASSWORD_RESET)
                .tokenHash(OpaqueTokens.hash(token))
                .expiresAt(now.plus(settings.auth().passwordResetTtl()))
                .build());
        emailSender.send(emails.passwordReset(user, token));
    }

    /** Define la nueva contraseña y cierra todas las sesiones abiertas. */
    @Transactional
    public void reset(String rawToken, String newPassword) {
        Instant now = clock.instant();
        UserToken token = tokens.findByHash(OpaqueTokens.hash(rawToken))
                .filter(t -> t.getTokenType() == UserTokenType.PASSWORD_RESET)
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new ServiceException(EmailVerificationUseCase.INVALID_LINK));

        User user = users.findById(token.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new ServiceException(EmailVerificationUseCase.INVALID_LINK));

        user.setPasswordHash(passwordHasher.hash(newPassword));
        // El enlace llegó a su casilla: el email queda verificado
        user.verifyEmail(now);
        users.save(user);

        token.setUsedAt(now);
        tokens.save(token);
        tokens.invalidateAll(user.getId(), UserTokenType.PASSWORD_RESET, now);
        tokens.invalidateAll(user.getId(), UserTokenType.REFRESH, now);
    }

    /**
     * Cambio de contraseña con sesión iniciada. Cierra las demás sesiones y devuelve una nueva.
     * Responde 400 (no 401) si la contraseña actual es incorrecta, para no confundirse con
     * una sesión vencida.
     */
    @Transactional
    public AuthSession change(UUID userId, String currentPassword, String newPassword) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("usuario"));
        if (user.hasPassword() && (currentPassword == null || !passwordHasher.matches(currentPassword, user.getPasswordHash()))) {
            throw new ServiceException("La contraseña actual es incorrecta");
        }

        user.setPasswordHash(passwordHasher.hash(newPassword));
        users.save(user);
        tokens.invalidateAll(user.getId(), UserTokenType.REFRESH, clock.instant());
        return sessionIssuer.issue(user);
    }
}
