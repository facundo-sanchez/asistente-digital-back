package org.acme.user.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.ConflictException;
import org.acme.user.application.model.AuthSession;
import org.acme.user.application.model.RegisterCommand;
import org.acme.user.application.port.output.PasswordHasher;
import org.acme.user.application.port.output.UserRepository;
import org.acme.user.domain.model.User;

/** Registro con email y contraseña. Deja la sesión iniciada y envía el email de verificación. */
@ApplicationScoped
public class RegisterUserUseCase {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final EmailVerificationUseCase emailVerification;
    private final SessionIssuer sessionIssuer;

    @Inject
    public RegisterUserUseCase(UserRepository users, PasswordHasher passwordHasher,
                               EmailVerificationUseCase emailVerification, SessionIssuer sessionIssuer) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.emailVerification = emailVerification;
        this.sessionIssuer = sessionIssuer;
    }

    @Transactional
    public AuthSession register(RegisterCommand command) {
        String email = User.normalizeEmail(command.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Ya existe una cuenta con ese email");
        }

        User user = users.save(User.builder()
                .email(email)
                .name(command.name().trim())
                .passwordHash(passwordHasher.hash(command.password()))
                .build());

        emailVerification.sendVerification(user);
        return sessionIssuer.issue(user);
    }
}
