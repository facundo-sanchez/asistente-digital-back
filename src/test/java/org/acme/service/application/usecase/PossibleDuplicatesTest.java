package org.acme.service.application.usecase;

import org.acme.service.application.model.ServiceView;
import org.acme.service.domain.model.CustomerReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Aviso de posible duplicado: mismo proveedor y mismo nro. de socio, escrito como sea. */
class PossibleDuplicatesTest {

    private ServiceFixture f;
    private UUID light;

    @BeforeEach
    void setUp() {
        f = new ServiceFixture();
        light = f.register.register(ServiceFixture.ANA, ServiceFixture.draft(f.coopelectric, "Luz casa").toBuilder()
                .customerReference("10.234-5").build()).service().getId();
    }

    @Test
    void normalizaElNroDeSocio() {
        assertEquals("102345", CustomerReference.normalize(" 10.234-5 "));
        assertEquals("ab12", CustomerReference.normalize("AB 12"));
        assertNull(CustomerReference.normalize(" - "));
        assertNull(CustomerReference.normalize(null));
    }

    @Test
    void encuentraElMismoSocioEscritoDistinto() {
        List<ServiceView> duplicates = f.query.possibleDuplicates(ServiceFixture.ANA, f.coopelectric.getId(), "102345", null);

        assertEquals(1, duplicates.size());
        assertEquals("Luz casa", duplicates.getFirst().service().getName());
        assertEquals("Coopelectric", duplicates.getFirst().providerName());
    }

    @Test
    void noEsDuplicadoConOtroProveedorOtroSocioOSinSocio() {
        assertTrue(f.query.possibleDuplicates(ServiceFixture.ANA, f.metrogas.getId(), "102345", null).isEmpty());
        assertTrue(f.query.possibleDuplicates(ServiceFixture.ANA, f.coopelectric.getId(), "999", null).isEmpty());
        assertTrue(f.query.possibleDuplicates(ServiceFixture.ANA, f.coopelectric.getId(), "  ", null).isEmpty());
    }

    @Test
    void alEditarNoSeAvisaSobreElMismoServicio() {
        assertTrue(f.query.possibleDuplicates(ServiceFixture.ANA, f.coopelectric.getId(), "102345", light).isEmpty());
    }

    @Test
    void noMiraLosServiciosDeOtroUsuario() {
        assertTrue(f.query.possibleDuplicates(ServiceFixture.BETO, f.coopelectric.getId(), "102345", null).isEmpty());
    }
}
