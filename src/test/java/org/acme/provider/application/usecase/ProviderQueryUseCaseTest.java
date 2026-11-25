package org.acme.provider.application.usecase;

import org.acme.domain.exceptions.NotFoundException;
import org.acme.provider.domain.model.Provider;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;
import org.acme.shared.domain.model.ServiceCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Catálogo de proveedores: visibilidad por usuario, filtros y paginación. */
class ProviderQueryUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();
    private static final PageQuery FIRST_PAGE = new PageQuery(0, 20);

    private InMemoryProviderRepository repository;
    private ProviderQueryUseCase useCase;
    private Provider anasOwn;
    private Provider betosOwn;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProviderRepository();
        useCase = new ProviderQueryUseCase(repository);
        repository.add(catalog("Coopelectric", ServiceCategory.ELECTRICITY));
        repository.add(catalog("Edenor", ServiceCategory.ELECTRICITY));
        repository.add(catalog("Metrogas", ServiceCategory.GAS));
        repository.add(catalog("Proveedor dado de baja", ServiceCategory.GAS).toBuilder().active(false).build());
        anasOwn = repository.add(own(ANA, "Cooperativa de Agua X", ServiceCategory.WATER));
        betosOwn = repository.add(own(BETO, "Gimnasio de Beto", ServiceCategory.OTHER));
    }

    @Test
    void listaElCatalogoActivoYLosPropiosPeroNuncaLosDeOtroUsuario() {
        List<String> names = names(useCase.list(ANA, null, null, FIRST_PAGE));

        assertEquals(List.of("Coopelectric", "Cooperativa de Agua X", "Edenor", "Metrogas"), names);
    }

    @Test
    void filtraPorParteDelNombreSinDistinguirMayusculas() {
        assertEquals(List.of("Coopelectric", "Cooperativa de Agua X"), names(useCase.list(ANA, "  COOPE ", null, FIRST_PAGE)));
    }

    @Test
    void filtraPorCategoria() {
        assertEquals(List.of("Metrogas"), names(useCase.list(ANA, null, ServiceCategory.GAS, FIRST_PAGE)));
    }

    @Test
    void textoEnBlancoEsComoNoFiltrar() {
        useCase.list(ANA, "   ", null, FIRST_PAGE);

        assertNull(repository.lastFilter().text());
    }

    @Test
    void normalizaLosEspaciosDelTexto() {
        assertEquals("agua x", ProviderQueryUseCase.normalize("  agua   x "));
    }

    @Test
    void paginaLosResultados() {
        PageResult<Provider> second = useCase.list(ANA, null, null, new PageQuery(1, 3));

        assertEquals(List.of("Metrogas"), names(second));
        assertEquals(4, second.totalElements());
    }

    @Test
    void consultaUnProveedorPropio() {
        assertEquals("Cooperativa de Agua X", useCase.get(ANA, anasOwn.getId()).getName());
    }

    @Test
    void unProveedorDeOtroUsuarioRespondeComoInexistente() {
        assertThrows(NotFoundException.class, () -> useCase.get(ANA, betosOwn.getId()));
        assertThrows(NotFoundException.class, () -> useCase.get(ANA, UUID.randomUUID()));
    }

    private static Provider catalog(String name, ServiceCategory category) {
        return Provider.builder().name(name).code(name.toUpperCase()).category(category).build();
    }

    private static Provider own(UUID userId, String name, ServiceCategory category) {
        return catalog(name, category).toBuilder().userId(userId).build();
    }

    private static List<String> names(PageResult<Provider> page) {
        return page.content().stream().map(Provider::getName).toList();
    }
}
