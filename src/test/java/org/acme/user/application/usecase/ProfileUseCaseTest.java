package org.acme.user.application.usecase;

import org.acme.domain.exceptions.ServiceException;
import org.acme.user.application.model.AuthSession;
import org.acme.user.application.model.UpdateProfileCommand;
import org.acme.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileUseCaseTest {

    private AuthFixture f;
    private AuthSession session;

    @BeforeEach
    void setUp() {
        f = new AuthFixture();
        session = f.registerDefault();
    }

    @Test
    void actualizaDatos() {
        User u = f.profile.update(session.user().getId(),
                new UpdateProfileCommand("Facu", "America/Argentina/Cordoba", "es-AR", "USD"));

        assertEquals("Facu", u.getName());
        assertEquals("America/Argentina/Cordoba", u.getTimezone());
        assertEquals("USD", u.getDefaultCurrency());
    }

    @Test
    void rechazaZonaHorariaInvalida() {
        assertThrows(ServiceException.class, () -> f.profile.update(session.user().getId(),
                new UpdateProfileCommand("Facu", "Marte/Olympus", "es-AR", "ARS")));
    }

    @Test
    void rechazaMonedaInvalida() {
        assertThrows(ServiceException.class, () -> f.profile.update(session.user().getId(),
                new UpdateProfileCommand("Facu", "America/Argentina/Buenos_Aires", "es-AR", "XYZ")));
    }
}
