package org.acme.user.application.usecase;

import org.acme.domain.exceptions.ServiceException;
import org.acme.user.application.model.AuthSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class EmailVerificationUseCaseTest {

    private AuthFixture f;

    @BeforeEach
    void setUp() {
        f = new AuthFixture();
    }

    @Test
    void verificaConElEnlaceRecibido() {
        AuthSession s = f.registerDefault();

        f.emailVerification.verify(f.tokenFromLastEmail());

        assertTrue(f.users.stored(s.user().getId()).isEmailVerified());
    }

    @Test
    void elEnlaceNoSePuedeUsarDosVeces() {
        f.registerDefault();
        String token = f.tokenFromLastEmail();
        f.emailVerification.verify(token);

        assertThrows(ServiceException.class, () -> f.emailVerification.verify(token));
    }

    @Test
    void enlaceVencido() {
        f.registerDefault();
        f.clock.advance(Duration.ofDays(3));

        assertThrows(ServiceException.class, () -> f.emailVerification.verify(f.tokenFromLastEmail()));
    }

    @Test
    void reenviarInvalidaElEnlaceAnterior() {
        AuthSession s = f.registerDefault();
        String old = f.tokenFromLastEmail();

        f.emailVerification.resend(s.user().getId());

        assertThrows(ServiceException.class, () -> f.emailVerification.verify(old));
        assertDoesNotThrow(() -> f.emailVerification.verify(f.tokenFromLastEmail()));
    }
}
