package org.acme.service.application.usecase;

import org.acme.provider.application.usecase.InMemoryProviderRepository;
import org.acme.provider.domain.model.Provider;
import org.acme.service.domain.model.Service;
import org.acme.shared.domain.model.BillingFrequency;
import org.acme.shared.domain.model.ServiceCategory;

import java.util.UUID;

/** Casos de uso de servicios armados con repositorios en memoria y un catálogo chico. */
public class ServiceFixture {

    static final UUID ANA = UUID.randomUUID();
    static final UUID BETO = UUID.randomUUID();

    final InMemoryProviderRepository providers = new InMemoryProviderRepository();
    final InMemoryServiceRepository services = new InMemoryServiceRepository();
    final ServiceDraftValidator validator = new ServiceDraftValidator(providers);
    final RegisterServiceUseCase register = new RegisterServiceUseCase(services, validator);
    final ManageServiceUseCase manage = new ManageServiceUseCase(services, providers, validator);
    final ServiceQueryUseCase query = new ServiceQueryUseCase(services, providers);

    final Provider coopelectric = providers.add(Provider.builder()
            .name("Coopelectric").code("COOPELECTRIC").category(ServiceCategory.ELECTRICITY).build());
    final Provider metrogas = providers.add(Provider.builder()
            .name("Metrogas").code("METROGAS").category(ServiceCategory.GAS).build());
    final Provider betosOwn = providers.add(Provider.builder()
            .userId(BETO).name("Gimnasio de Beto").code("GYM_BETO").category(ServiceCategory.OTHER).build());

    /** Borrador como el que arma el mapper a partir del formulario. */
    static Service draft(Provider provider, String name) {
        return Service.builder()
                .providerId(provider.getId())
                .name(name)
                .category(provider.getCategory())
                .billingFrequency(BillingFrequency.MONTHLY)
                .build();
    }
}
