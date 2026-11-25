package org.acme.provider.application.usecase;

import org.acme.domain.exceptions.ConflictException;
import org.acme.provider.domain.model.Provider;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.domain.model.ServiceCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Proveedores propios: dueño, code generado, nombre repetido y visibilidad. */
class CreateProviderUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();

    private InMemoryProviderRepository repository;
    private CreateProviderUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProviderRepository();
        useCase = new CreateProviderUseCase(repository);
        repository.add(Provider.builder().name("Coopelectric").code("COOPELECTRIC").category(ServiceCategory.ELECTRICITY).build());
    }

    @Test
    void creaUnProveedorPropioActivoConCodeLegible() {
        Provider created = useCase.create(ANA, draft("  Cooperativa  de Agua Ñandú ", " https://agua.example.com "));

        assertEquals(ANA, created.getUserId());
        assertTrue(created.isCustom());
        assertTrue(created.isActive());
        assertEquals("Cooperativa de Agua Ñandú", created.getName());
        assertEquals("COOPERATIVA_DE_AGUA_NANDU", created.getCode());
        assertEquals("https://agua.example.com", created.getWebsite());
    }

    @Test
    void rechazaUnNombreQueYaVeEnElCatalogoOEntreLosPropios() {
        assertThrows(ConflictException.class, () -> useCase.create(ANA, draft("COOPELECTRIC", null)));

        useCase.create(ANA, draft("Agua X", null));
        assertThrows(ConflictException.class, () -> useCase.create(ANA, draft("agua x", null)));
    }

    @Test
    void otroUsuarioPuedeUsarElMismoNombreYNoVeElAjeno() {
        Provider anas = useCase.create(ANA, draft("Agua X", null));

        assertDoesNotThrow(() -> useCase.create(BETO, draft("Agua X", null)));
        assertTrue(repository.findVisibleById(BETO, anas.getId()).isEmpty());
        assertEquals(2, new ProviderQueryUseCase(repository).list(BETO, null, null, new PageQuery(0, 20)).totalElements());
    }

    @Test
    void generaUnCodeUnicoSiOtroNombreDaElMismoCode() {
        useCase.create(ANA, draft("Agua-X", null));

        assertEquals("AGUA_X_2", useCase.create(ANA, draft("Agua X!", null)).getCode());
    }

    @Test
    void unNombreSinLetrasNiNumerosTieneUnCodeDeRespaldo() {
        assertEquals("PROVEEDOR", CreateProviderUseCase.toCode("¡¿?!"));
    }

    private static Provider draft(String name, String website) {
        return Provider.builder().name(name).category(ServiceCategory.WATER).website(website).build();
    }
}
