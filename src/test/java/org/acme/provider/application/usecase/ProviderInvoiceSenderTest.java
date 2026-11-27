package org.acme.provider.application.usecase;

import org.acme.domain.exceptions.ConflictException;
import org.acme.provider.domain.model.Provider;
import org.acme.shared.domain.model.ServiceCategory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProviderInvoiceSenderTest {

    private final InMemoryProviderRepository providers = new InMemoryProviderRepository();
    private final CreateProviderUseCase useCase = new CreateProviderUseCase(providers);
    private final UUID ana = UUID.randomUUID();

    @Test
    void guardaElRemitenteEnMinusculasYSePuedeCambiarOQuitar() {
        Provider nativa = useCase.create(ana, draft("Nativa", "  Info@NativaSeguros.com.ar "));
        assertEquals("info@nativaseguros.com.ar", nativa.getInvoiceSender());
        assertEquals("info@nativaseguros.com.ar", providers.findInvoiceSender(nativa.getId()).orElseThrow());

        useCase.update(ana, nativa.getId(), draft("Nativa Seguros", "nativaseguros.com.ar"));
        assertEquals("nativaseguros.com.ar", providers.findInvoiceSender(nativa.getId()).orElseThrow());

        useCase.update(ana, nativa.getId(), draft("Nativa Seguros", ""));
        assertTrue(providers.findInvoiceSender(nativa.getId()).isEmpty());
    }

    @Test
    void noSeEditaElProveedorDeOtroUsuario() {
        Provider nativa = useCase.create(ana, draft("Nativa", null));

        assertThrows(RuntimeException.class, () -> useCase.update(UUID.randomUUID(), nativa.getId(), draft("Mía", null)));
    }

    @Test
    void losDelCatalogoNoSeEditan() {
        Provider catalog = providers.save(Provider.builder().name("Camuzzi").code("CAMUZZI").category(ServiceCategory.values()[0]).build());

        assertThrows(ConflictException.class, () -> useCase.update(ana, catalog.getId(), draft("Otro", null)));
    }

    private static Provider draft(String name, String sender) {
        return Provider.builder().name(name).category(ServiceCategory.values()[0]).invoiceSender(sender).build();
    }
}
