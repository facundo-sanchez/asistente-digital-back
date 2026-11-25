package org.acme.user.application.usecase;

import org.acme.domain.exceptions.ConflictException;
import org.acme.user.application.model.AuthSession;
import org.acme.user.application.model.RegisterCommand;
import org.acme.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterUserUseCaseTest {

    private AuthFixture f;

    @BeforeEach
    void setUp() {
        f = new AuthFixture();
    }

    @Test
    void creaUsuarioNormalizadoConHashYSesion() {
        AuthSession session = f.registerDefault();

        User user = session.user();
        assertEquals(AuthFixture.EMAIL, user.getEmail());
        assertEquals("Facundo", user.getName());
        assertEquals("h:" + AuthFixture.PASSWORD, f.users.stored(user.getId()).getPasswordHash());
        assertFalse(user.isEmailVerified());
        assertNotNull(session.accessToken());
        assertNotNull(session.refreshToken());
        assertEquals(1, f.tokens.activeRefreshTokens(user.getId()));
    }

    @Test
    void enviaEmailDeVerificacionConEnlace() {
        f.registerDefault();

        assertEquals(AuthFixture.EMAIL, f.emails.last().to());
        assertTrue(f.emails.last().body().contains("http://localhost:3000/verify-email?token="));
    }

    @Test
    void rechazaEmailDuplicadoSinImportarMayusculas() {
        f.registerDefault();

        assertThrows(ConflictException.class,
                () -> f.register.register(new RegisterCommand("Otro", "USUARIO@mail.com", "otra1234")));
    }

    @Test
    void elRefreshTokenSeGuardaHasheado() {
        AuthSession session = f.registerDefault();

        assertTrue(f.tokens.all().stream().noneMatch(t -> t.getTokenHash().equals(session.refreshToken())));
    }
}
