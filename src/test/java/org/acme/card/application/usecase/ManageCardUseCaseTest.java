package org.acme.card.application.usecase;

import org.acme.card.application.model.CardView;
import org.acme.card.domain.model.Card;
import org.acme.card.domain.model.CardBrand;
import org.acme.domain.exceptions.ConflictException;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.service.application.usecase.InMemoryServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.shared.domain.model.ServiceCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Tarjetas: servicio del emisor, repetidas, edición, baja lógica y aislamiento. */
class ManageCardUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();

    private ManageCardUseCase useCase;
    private UUID galicia;
    private UUID light;
    private UUID betoBank;

    @BeforeEach
    void setUp() {
        InMemoryServiceRepository services = new InMemoryServiceRepository();
        galicia = service(services, ANA, "Galicia", ServiceCategory.CREDIT_CARD);
        light = service(services, ANA, "Luz casa", ServiceCategory.ELECTRICITY);
        betoBank = service(services, BETO, "BBVA Beto", ServiceCategory.CREDIT_CARD);
        useCase = new ManageCardUseCase(new InMemoryCardRepository(), services);
    }

    @Test
    void registraConElNombreDelBancoYSeEditaSinChocarConsigoMisma() {
        CardView view = useCase.register(ANA, card(galicia, "  Visa Galicia ", "1234"));

        assertEquals("Visa Galicia", view.card().getName());
        assertEquals("Galicia", view.serviceName());
        assertTrue(view.card().isActive());
        assertEquals("Visa oro", useCase.update(ANA, view.card().getId(), card(galicia, "Visa oro", "1234")).card().getName());
    }

    @Test
    void elServicioTieneQueSerDeTarjetasYDelUsuario() {
        assertThrows(ServiceException.class, () -> useCase.register(ANA, card(light, "Visa", "1234")));
        assertThrows(ServiceException.class, () -> useCase.register(ANA, card(betoBank, "Visa", "1234")));
    }

    @Test
    void laMismaMarcaYTerminacionNoSeRepiteParaElUsuario() {
        useCase.register(ANA, card(galicia, "Visa Galicia", "1234"));

        assertThrows(ConflictException.class, () -> useCase.register(ANA, card(galicia, "Otra Visa", "1234")));
        assertNotNull(useCase.register(ANA, card(galicia, "Visa sin número", null)));
    }

    @Test
    void laBajaEsLogicaYCadaUsuarioVeLasSuyas() {
        UUID id = useCase.register(ANA, card(galicia, "Visa Galicia", "1234")).card().getId();

        useCase.changeStatus(ANA, id, false);

        assertEquals(List.of(), useCase.list(ANA, true));
        assertEquals(1, useCase.list(ANA, null).size());
        assertTrue(useCase.list(BETO, null).isEmpty());
        assertThrows(NotFoundException.class, () -> useCase.get(BETO, id));
    }

    private static UUID service(InMemoryServiceRepository services, UUID userId, String name, ServiceCategory category) {
        return services.save(Service.builder().userId(userId).providerId(UUID.randomUUID()).name(name).category(category).build()).getId();
    }

    private static Card card(UUID serviceId, String name, String lastFour) {
        return Card.builder().serviceId(serviceId).name(name).brand(CardBrand.VISA).lastFour(lastFour).build();
    }
}
