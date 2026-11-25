package org.acme.service.application.usecase;

import org.acme.domain.exceptions.ServiceException;
import org.acme.service.application.model.ServiceView;
import org.acme.service.domain.model.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Alta de servicios (CU-001): dueño, proveedor válido, fechas y limpieza de datos. */
class RegisterServiceUseCaseTest {

    private ServiceFixture f;

    @BeforeEach
    void setUp() {
        f = new ServiceFixture();
    }

    @Test
    void registraElServicioActivoParaElUsuarioConElNombreDelProveedor() {
        Service draft = ServiceFixture.draft(f.coopelectric, "  Luz casa ").toBuilder()
                .customerReference(" 10234 ")
                .notes("   ")
                .build();

        ServiceView view = f.register.register(ServiceFixture.ANA, draft);

        Service stored = f.services.stored(view.service().getId());
        assertEquals(ServiceFixture.ANA, stored.getUserId());
        assertEquals("Luz casa", stored.getName());
        assertEquals("10234", stored.getCustomerReference());
        assertNull(stored.getNotes(), "notas en blanco se guardan vacías");
        assertTrue(stored.isActive());
        assertNotNull(stored.getMetadata());
        assertEquals("Coopelectric", view.providerName());
    }

    @Test
    void ignoraElIdYElDuenoQueVengaEnElBorrador() {
        Service draft = ServiceFixture.draft(f.metrogas, "Gas").toBuilder()
                .id(java.util.UUID.randomUUID())
                .userId(ServiceFixture.BETO)
                .active(false)
                .build();

        Service stored = f.register.register(ServiceFixture.ANA, draft).service();

        assertNotEquals(draft.getId(), stored.getId());
        assertEquals(ServiceFixture.ANA, stored.getUserId());
        assertTrue(stored.isActive());
    }

    @Test
    void rechazaUnProveedorPropioDeOtroUsuario() {
        ServiceException e = assertThrows(ServiceException.class,
                () -> f.register.register(ServiceFixture.ANA, ServiceFixture.draft(f.betosOwn, "Gimnasio")));

        assertEquals(ServiceDraftValidator.UNKNOWN_PROVIDER, e.getMessage());
    }

    @Test
    void rechazaUnProveedorDadoDeBaja() {
        f.coopelectric.setActive(false);
        var inactive = f.providers.add(f.coopelectric);

        ServiceException e = assertThrows(ServiceException.class,
                () -> f.register.register(ServiceFixture.ANA, ServiceFixture.draft(inactive, "Luz")));

        assertEquals(ServiceDraftValidator.INACTIVE_PROVIDER, e.getMessage());
    }

    @Test
    void rechazaUnaFechaDeFinAnteriorALaDeInicio() {
        Service draft = ServiceFixture.draft(f.coopelectric, "Luz").toBuilder()
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 4, 30))
                .build();

        ServiceException e = assertThrows(ServiceException.class, () -> f.register.register(ServiceFixture.ANA, draft));

        assertEquals(ServiceDraftValidator.INVALID_DATES, e.getMessage());
    }
}
