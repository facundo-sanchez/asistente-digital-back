package org.acme.user.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.shared.application.AppSettings;
import org.acme.shared.application.port.output.EmailSender;
import org.acme.user.application.port.output.UserRepository;
import org.acme.user.application.port.output.UserTokenRepository;
import org.acme.user.application.security.OpaqueTokens;
import org.acme.user.domain.model.User;
import org.acme.user.domain.model.UserToken;
import org.acme.user.domain.model.UserTokenType;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Verificación de email: envío del enlace y confirmación. */
@ApplicationScoped
public class EmailVerificationUseCase {

    static final String INVALID_LINK = "El enlace no es válido o expiró";

    private final UserRepository users;
    private final UserTokenRepository tokens;
    private final EmailSender emailSender;
    private final AuthEmails emails;
    private final AppSettings settings;
    private final Clock clock;

    @Inject
    public EmailVerificationUseCase(UserRepository users, UserTokenRepository tokens, EmailSender emailSender,
                                    AuthEmails emails, AppSettings settings, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.emailSender = emailSender;
        this.emails = emails;
        this.settings = settings;
        this.clock = clock;
    }

    /** Genera un enlace nuevo (invalida los anteriores) y lo envía. No hace nada si ya está verificado. */
    @Transactional
    public void sendVerification(User user) {
        if (user.isEmailVerified()) {
            return;
        }
        Instant now = clock.instant();
        tokens.invalidateAll(user.getId(), UserTokenType.EMAIL_VERIFICATION, now);
        String token = OpaqueTokens.generate();
        tokens.save(UserToken.builder()
                .userId(user.getId())
                .tokenType(UserTokenType.EMAIL_VERIFICATION)
                .tokenHash(OpaqueTokens.hash(token))
                .expiresAt(now.plus(settings.auth().emailVerificationTtl()))
                .build());
        emailSender.send(emails.verification(user, token));
    }

    @Transactional
    public void resend(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("usuario"));
        sendVerification(user);
    }

    @Transactional
    public void verify(String rawToken) {
        Instant now = clock.instant();
        UserToken token = tokens.findByHash(OpaqueTokens.hash(rawToken))
                .filter(t -> t.getTokenType() == UserTokenType.EMAIL_VERIFICATION)
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new ServiceException(INVALID_LINK));

        User user = users.findById(token.getUserId()).orElseThrow(() -> new ServiceException(INVALID_LINK));
        user.verifyEmail(now);
        users.save(user);

        token.setUsedAt(now);
        tokens.save(token);
    }
}
