package org.acme.user.application.usecase;

import org.acme.domain.exceptions.NotAuthorizedException;
import org.acme.domain.exceptions.UnavailableException;
import org.acme.user.application.model.AuthSession;
import org.acme.user.application.model.GoogleIdentity;
import org.acme.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Login con Google: alta, vuelta, vinculación con una cuenta existente y rechazos. */
class GoogleLoginUseCaseTest {

    private static final String SUBJECT = "google-sub-123";
    private static final String VERIFIER = "v".repeat(43);

    private AuthFixture f;

    @BeforeEach
    void setUp() {
        f = new AuthFixture();
    }

    @Test
    void primerIngresoCreaUnaCuentaVerificadaSinContrasena() {
        f.google.willReturn(new GoogleIdentity(SUBJECT, "Ana@Gmail.com", true, "Ana Pérez"));

        AuthSession session = login();

        User user = f.users.stored(session.user().getId());
        assertEquals("ana@gmail.com", user.getEmail());
        assertEquals("Ana Pérez", user.getName());
        assertEquals(SUBJECT, user.getGoogleSubject());
        assertTrue(user.isEmailVerified());
        assertFalse(user.hasPassword());
        assertNotNull(user.getLastLoginAt());
        assertNotNull(session.refreshToken());
    }

    @Test
    void sinNombreEnGoogleUsaLaParteLocalDelEmail() {
        f.google.willReturn(new GoogleIdentity(SUBJECT, "ana@gmail.com", true, " "));

        assertEquals("ana", login().user().getName());
    }

    @Test
    void segundoIngresoEntraALaMismaCuenta() {
        f.google.willReturn(new GoogleIdentity(SUBJECT, "ana@gmail.com", true, "Ana"));
        AuthSession first = login();

        AuthSession second = login();

        assertEquals(first.user().getId(), second.user().getId());
    }

    @Test
    void vinculaLaCuentaVerificadaConElMismoEmailYConservaLaContrasena() {
        AuthSession registered = f.registerDefault();
        f.users.stored(registered.user().getId()).verifyEmail(f.clock.instant());
        f.google.willReturn(new GoogleIdentity(SUBJECT, AuthFixture.EMAIL, true, "Facundo"));

        AuthSession session = login();

        assertEquals(registered.user().getId(), session.user().getId());
        User user = f.users.stored(session.user().getId());
        assertEquals(SUBJECT, user.getGoogleSubject());
        assertTrue(user.hasPassword());
        // La sesión anterior sigue abierta
        assertDoesNotThrow(() -> f.authentication.refresh(registered.refreshToken()));
    }

    @Test
    void alVincularUnaCuentaSinVerificarDescartaSuContrasenaYCierraSusSesiones() {
        // Alguien registró el email antes que su dueño, sin poder verificarlo
        AuthSession intruder = f.registerDefault();
        f.google.willReturn(new GoogleIdentity(SUBJECT, AuthFixture.EMAIL, true, "Dueño Real"));

        AuthSession owner = login();

        User user = f.users.stored(owner.user().getId());
        assertFalse(user.hasPassword(), "la contraseña del intruso no debe servir más");
        assertEquals("Dueño Real", user.getName());
        assertTrue(user.isEmailVerified());
        assertThrows(NotAuthorizedException.class, () -> f.authentication.refresh(intruder.refreshToken()));
        assertThrows(NotAuthorizedException.class,
                () -> f.authentication.login(AuthFixture.EMAIL, AuthFixture.PASSWORD));
    }

    @Test
    void rechazaUnEmailDeGoogleSinVerificar() {
        f.google.willReturn(new GoogleIdentity(SUBJECT, "ana@gmail.com", false, "Ana"));

        NotAuthorizedException e = assertThrows(NotAuthorizedException.class, this::login);
        assertEquals(GoogleLoginUseCase.UNVERIFIED_EMAIL, e.getMessage());
    }

    @Test
    void rechazaSiLaCuentaYaEstaVinculadaAOtraCuentaDeGoogle() {
        f.google.willReturn(new GoogleIdentity("otro-sub", "ana@gmail.com", true, "Ana"));
        login();
        f.google.willReturn(new GoogleIdentity(SUBJECT, "ana@gmail.com", true, "Ana"));

        NotAuthorizedException e = assertThrows(NotAuthorizedException.class, this::login);
        assertEquals(GoogleLoginUseCase.OTHER_GOOGLE_ACCOUNT, e.getMessage());
    }

    @Test
    void rechazaUnaCuentaDeshabilitada() {
        f.google.willReturn(new GoogleIdentity(SUBJECT, "ana@gmail.com", true, "Ana"));
        AuthSession session = login();
        f.users.stored(session.user().getId()).setActive(false);

        NotAuthorizedException e = assertThrows(NotAuthorizedException.class, this::login);
        assertEquals(GoogleLoginUseCase.DISABLED, e.getMessage());
    }

    @Test
    void elCallbackEsElDelBffBajoLaUrlDelFrontend() {
        f.googleLogin.authorizationUrl("s".repeat(16), "c".repeat(43));

        assertEquals("http://localhost:3000" + GoogleLoginUseCase.CALLBACK_PATH, f.google.lastRedirectUri());
    }

    @Test
    void sinCredencialesConfiguradasNoEstaDisponible() {
        GoogleLoginUseCase unconfigured = new GoogleLoginUseCase(f.google, f.users, f.tokens, f.sessionIssuer,
                new TestAppSettings(new TestGoogleSettings(false)), f.clock);

        assertThrows(UnavailableException.class, () -> unconfigured.authorizationUrl("s".repeat(16), "c".repeat(43)));
        assertThrows(UnavailableException.class, () -> unconfigured.login(FakeGoogleIdentityProvider.VALID_CODE, VERIFIER));
    }

    private AuthSession login() {
        return f.googleLogin.login(FakeGoogleIdentityProvider.VALID_CODE, VERIFIER);
    }
}
