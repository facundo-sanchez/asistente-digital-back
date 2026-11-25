package org.acme.user.application.usecase;

import org.acme.shared.application.AppSettings;
import org.acme.user.application.model.AuthSession;
import org.acme.user.application.model.RegisterCommand;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Casos de uso de autenticación armados con adaptadores en memoria. */
public class AuthFixture {

    public static final String EMAIL = "usuario@mail.com";
    public static final String PASSWORD = "secreta123";

    final MutableClock clock = new MutableClock(Instant.parse("2026-11-24T12:00:00Z"));
    final InMemoryUserRepository users = new InMemoryUserRepository(clock);
    final InMemoryUserTokenRepository tokens = new InMemoryUserTokenRepository();
    final FakePasswordHasher hasher = new FakePasswordHasher();
    final RecordingEmailSender emails = new RecordingEmailSender();
    final AppSettings settings = new TestAppSettings();

    final SessionIssuer sessionIssuer = new SessionIssuer(new FakeAccessTokenIssuer(settings), tokens, settings, clock);
    final AuthEmails authEmails = new AuthEmails(settings);
    final EmailVerificationUseCase emailVerification =
            new EmailVerificationUseCase(users, tokens, emails, authEmails, settings, clock);
    final RegisterUserUseCase register = new RegisterUserUseCase(users, hasher, emailVerification, sessionIssuer);
    final AuthenticationUseCase authentication = new AuthenticationUseCase(users, tokens, hasher, sessionIssuer, clock);
    final PasswordUseCase passwords =
            new PasswordUseCase(users, tokens, hasher, emails, authEmails, sessionIssuer, settings, clock);
    final ProfileUseCase profile = new ProfileUseCase(users);

    /** Registra el usuario de prueba con email y nombre "sucios" para probar la normalización. */
    AuthSession registerDefault() {
        return register.register(new RegisterCommand(" Facundo ", "  Usuario@Mail.com ", PASSWORD));
    }

    /** Token incluido en el último email enviado (?token=...). */
    String tokenFromLastEmail() {
        Matcher m = Pattern.compile("token=([A-Za-z0-9_\\-%]+)").matcher(emails.last().body());
        if (!m.find()) {
            throw new AssertionError("El email no tiene token: " + emails.last().body());
        }
        return URLDecoder.decode(m.group(1), StandardCharsets.UTF_8);
    }
}
