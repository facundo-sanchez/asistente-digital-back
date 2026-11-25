package org.acme.user.application.usecase;

import org.acme.domain.exceptions.NotAuthorizedException;
import org.acme.user.application.model.AuthSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** Login, refresh rotativo y logout. */
class AuthenticationUseCaseTest {

    private AuthFixture f;

    @BeforeEach
    void setUp() {
        f = new AuthFixture();
    }

    // ---------- Login ----------

    @Test
    void loginConCredencialesCorrectas() {
        f.registerDefault();

        AuthSession session = f.authentication.login("USUARIO@mail.com", AuthFixture.PASSWORD);

        assertEquals(AuthFixture.EMAIL, session.user().getEmail());
        assertNotNull(f.users.stored(session.user().getId()).getLastLoginAt());
    }

    @Test
    void loginConContrasenaIncorrecta() {
        f.registerDefault();

        NotAuthorizedException e = assertThrows(NotAuthorizedException.class,
                () -> f.authentication.login(AuthFixture.EMAIL, "mala1234"));
        assertEquals(AuthenticationUseCase.INVALID_CREDENTIALS, e.getMessage());
    }

    @Test
    void loginConEmailInexistenteDaElMismoMensajeYVerificaIgual() {
        int before = f.hasher.verifications();

        NotAuthorizedException e = assertThrows(NotAuthorizedException.class,
                () -> f.authentication.login("nadie@mail.com", AuthFixture.PASSWORD));

        assertEquals(AuthenticationUseCase.INVALID_CREDENTIALS, e.getMessage());
        assertEquals(before + 1, f.hasher.verifications(), "debe verificar contra un hash ficticio");
    }

    @Test
    void loginConCuentaDeshabilitada() {
        AuthSession s = f.registerDefault();
        f.users.stored(s.user().getId()).setActive(false);

        assertThrows(NotAuthorizedException.class, () -> f.authentication.login(AuthFixture.EMAIL, AuthFixture.PASSWORD));
    }

    // ---------- Refresh ----------

    @Test
    void refreshRotaElTokenYElAnteriorNoSirveMas() {
        AuthSession first = f.registerDefault();

        AuthSession second = f.authentication.refresh(first.refreshToken());

        assertNotEquals(first.refreshToken(), second.refreshToken());
        assertThrows(NotAuthorizedException.class, () -> f.authentication.refresh(first.refreshToken()));
        assertEquals(1, f.tokens.activeRefreshTokens(first.user().getId()), "la sesión nueva sigue viva");
    }

    @Test
    void reutilizacionFueraDeLaVentanaCierraTodasLasSesiones() {
        AuthSession first = f.registerDefault();
        f.authentication.refresh(first.refreshToken());
        f.clock.advance(AuthenticationUseCase.REUSE_GRACE.plusSeconds(1));

        assertThrows(NotAuthorizedException.class, () -> f.authentication.refresh(first.refreshToken()));
        assertEquals(0, f.tokens.activeRefreshTokens(first.user().getId()));
    }

    @Test
    void refreshConTokenVencido() {
        AuthSession first = f.registerDefault();
        f.clock.advance(Duration.ofDays(31));

        assertThrows(NotAuthorizedException.class, () -> f.authentication.refresh(first.refreshToken()));
    }

    @Test
    void refreshConTokenInexistente() {
        assertThrows(NotAuthorizedException.class, () -> f.authentication.refresh("no-existe"));
    }

    // ---------- Logout ----------

    @Test
    void logoutInvalidaElRefreshToken() {
        AuthSession s = f.registerDefault();

        f.authentication.logout(s.refreshToken());

        assertThrows(NotAuthorizedException.class, () -> f.authentication.refresh(s.refreshToken()));
    }
}
