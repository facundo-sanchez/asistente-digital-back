package org.acme.service.application.usecase;

import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.service.application.model.ServiceView;
import org.acme.service.domain.model.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Edición, activar/desactivar y baja lógica de servicios. */
class ManageServiceUseCaseTest {

    private ServiceFixture f;
    private UUID light;

    @BeforeEach
    void setUp() {
        f = new ServiceFixture();
        light = f.register.register(ServiceFixture.ANA, ServiceFixture.draft(f.coopelectric, "Luz casa")).service().getId();
    }

    @Test
    void editaLosDatosSinCambiarDuenoNiEstado() {
        Service draft = ServiceFixture.draft(f.metrogas, " Gas casa ").toBuilder()
                .customerReference("555")
                .userId(ServiceFixture.BETO)
                .active(false)
                .build();

        ServiceView view = f.manage.update(ServiceFixture.ANA, light, draft);

        Service stored = f.services.stored(light);
        assertEquals("Gas casa", stored.getName());
        assertEquals("555", stored.getCustomerReference());
        assertEquals(f.metrogas.getId(), stored.getProviderId());
        assertEquals(ServiceFixture.ANA, stored.getUserId());
        assertTrue(stored.isActive());
        assertEquals("Metrogas", view.providerName());
    }

    @Test
    void laEdicionValidaComoElAlta() {
        Service draft = ServiceFixture.draft(f.coopelectric, "Luz").toBuilder()
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 1, 1))
                .build();

        assertThrows(ServiceException.class, () -> f.manage.update(ServiceFixture.ANA, light, draft));
    }

    @Test
    void conservaUnProveedorQueSeDioDeBajaDespuesDelAlta() {
        f.coopelectric.setActive(false);

        assertDoesNotThrow(() -> f.manage.update(ServiceFixture.ANA, light, ServiceFixture.draft(f.coopelectric, "Luz nueva")));
    }

    @Test
    void desactivaYReactiva() {
        assertFalse(f.manage.changeStatus(ServiceFixture.ANA, light, false).service().isActive());
        assertTrue(f.manage.changeStatus(ServiceFixture.ANA, light, true).service().isActive());
    }

    @Test
    void laBajaEsLogicaYNoBorraElServicio() {
        f.manage.delete(ServiceFixture.ANA, light);

        assertNotNull(f.services.stored(light));
        assertFalse(f.services.stored(light).isActive());
    }

    @Test
    void otroUsuarioNoPuedeTocarElServicio() {
        Service draft = ServiceFixture.draft(f.coopelectric, "Robado");

        assertThrows(NotFoundException.class, () -> f.manage.update(ServiceFixture.BETO, light, draft));
        assertThrows(NotFoundException.class, () -> f.manage.changeStatus(ServiceFixture.BETO, light, false));
        assertThrows(NotFoundException.class, () -> f.manage.delete(ServiceFixture.BETO, light));
        assertEquals("Luz casa", f.services.stored(light).getName());
    }
}
