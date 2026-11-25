package org.acme.user.application.usecase;

import org.acme.domain.exceptions.NotAuthorizedException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.user.application.model.AuthSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** Recupero, reseteo y cambio de contraseña. */
class PasswordUseCaseTest {

    private AuthFixture f;

    @BeforeEach
    void setUp() {
        f = new AuthFixture();
    }

    @Test
    void recuperoConEmailInexistenteNoEnviaNada() {
        f.passwords.forgot("nadie@mail.com");

        assertTrue(f.emails.sent().isEmpty());
    }

    @Test
    void resetCambiaLaContrasenaYCierraLasSesiones() {
        AuthSession s = f.registerDefault();
        f.passwords.forgot("Usuario@Mail.com");

        f.passwords.reset(f.tokenFromLastEmail(), "nueva1234");

        assertEquals(0, f.tokens.activeRefreshTokens(s.user().getId()));
        assertThrows(NotAuthorizedException.class, () -> f.authentication.login(AuthFixture.EMAIL, AuthFixture.PASSWORD));
        assertNotNull(f.authentication.login(AuthFixture.EMAIL, "nueva1234"));
        assertTrue(f.users.stored(s.user().getId()).isEmailVerified(), "el enlace prueba la propiedad del email");
    }

    @Test
    void enlaceDeResetVencido() {
        f.registerDefault();
        f.passwords.forgot(AuthFixture.EMAIL);
        f.clock.advance(Duration.ofHours(2));

        assertThrows(ServiceException.class, () -> f.passwords.reset(f.tokenFromLastEmail(), "nueva1234"));
    }

    @Test
    void cambioConContrasenaActualIncorrectaEs400() {
        AuthSession s = f.registerDefault();

        assertThrows(ServiceException.class,
                () -> f.passwords.change(s.user().getId(), "mala1234", "nueva1234"));
    }

    @Test
    void cambioDevuelveSesionNuevaYCierraLasDemas() {
        AuthSession s = f.registerDefault();

        AuthSession nueva = f.passwords.change(s.user().getId(), AuthFixture.PASSWORD, "nueva1234");

        assertThrows(NotAuthorizedException.class, () -> f.authentication.refresh(s.refreshToken()));
        assertNotNull(f.authentication.refresh(nueva.refreshToken()));
    }
}
