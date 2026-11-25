package org.acme.service.application.usecase;

import org.acme.domain.exceptions.NotFoundException;
import org.acme.service.application.model.ServiceView;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;
import org.acme.shared.domain.model.ServiceCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Listado y detalle de servicios: aislamiento entre usuarios, filtros y nombre del proveedor. */
class ServiceQueryUseCaseTest {

    private static final PageQuery FIRST_PAGE = new PageQuery(0, 20);

    private ServiceFixture f;
    private UUID anasLight;

    @BeforeEach
    void setUp() {
        f = new ServiceFixture();
        anasLight = f.register.register(ServiceFixture.ANA, ServiceFixture.draft(f.coopelectric, "Luz casa").toBuilder()
                .customerReference("10234").build()).service().getId();
        f.register.register(ServiceFixture.ANA, ServiceFixture.draft(f.metrogas, "Gas casa"));
        f.register.register(ServiceFixture.BETO, ServiceFixture.draft(f.coopelectric, "Luz de Beto"));
    }

    @Test
    void cadaUsuarioVeSoloSusServiciosConElNombreDelProveedor() {
        PageResult<ServiceView> page = f.query.list(ServiceFixture.ANA, null, null, null, FIRST_PAGE);

        assertEquals(List.of("Gas casa", "Luz casa"), names(page));
        assertEquals(List.of("Metrogas", "Coopelectric"), page.content().stream().map(ServiceView::providerName).toList());
    }

    @Test
    void buscaPorNombreOPorNroDeSocio() {
        assertEquals(List.of("Luz casa"), names(f.query.list(ServiceFixture.ANA, " LUZ ", null, null, FIRST_PAGE)));
        assertEquals(List.of("Luz casa"), names(f.query.list(ServiceFixture.ANA, "1023", null, null, FIRST_PAGE)));
    }

    @Test
    void filtraPorCategoriaYEstado() {
        assertEquals(List.of("Gas casa"), names(f.query.list(ServiceFixture.ANA, null, ServiceCategory.GAS, null, FIRST_PAGE)));

        f.services.stored(anasLight).setActive(false);
        assertEquals(List.of("Gas casa"), names(f.query.list(ServiceFixture.ANA, null, null, true, FIRST_PAGE)));
        assertEquals(List.of("Luz casa"), names(f.query.list(ServiceFixture.ANA, null, null, false, FIRST_PAGE)));
    }

    @Test
    void consultaElDetalleDeUnServicioPropio() {
        ServiceView view = f.query.get(ServiceFixture.ANA, anasLight);

        assertEquals("Luz casa", view.service().getName());
        assertEquals("Coopelectric", view.providerName());
    }

    @Test
    void unServicioDeOtroUsuarioRespondeComoInexistente() {
        assertThrows(NotFoundException.class, () -> f.query.get(ServiceFixture.BETO, anasLight));
        assertThrows(NotFoundException.class, () -> f.query.get(ServiceFixture.ANA, UUID.randomUUID()));
    }

    private static List<String> names(PageResult<ServiceView> page) {
        return page.content().stream().map(v -> v.service().getName()).toList();
    }
}
