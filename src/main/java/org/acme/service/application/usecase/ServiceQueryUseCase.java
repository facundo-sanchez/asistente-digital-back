package org.acme.service.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.provider.application.port.output.ProviderRepository;
import org.acme.service.application.model.ServiceFilter;
import org.acme.service.application.model.ServiceView;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.shared.application.SearchText;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;
import org.acme.shared.domain.model.ServiceCategory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Consulta de los servicios del usuario, con el nombre de su proveedor. */
@ApplicationScoped
public class ServiceQueryUseCase {

    private final ServiceRepository services;
    private final ProviderRepository providers;

    @Inject
    public ServiceQueryUseCase(ServiceRepository services, ProviderRepository providers) {
        this.services = services;
        this.providers = providers;
    }

    public PageResult<ServiceView> list(UUID userId, String text, ServiceCategory category, Boolean active, PageQuery page) {
        PageResult<Service> result = services.findForUser(userId, new ServiceFilter(SearchText.normalize(text), category, active), page);
        // Un solo viaje a la base para los nombres de todos los proveedores de la página
        Set<UUID> providerIds = result.content().stream().map(Service::getProviderId).collect(Collectors.toSet());
        Map<UUID, String> names = providers.findNames(providerIds);
        return result.map(service -> new ServiceView(service, names.get(service.getProviderId())));
    }

    /** Un servicio de otro usuario responde igual que uno inexistente. */
    public ServiceView get(UUID userId, UUID serviceId) {
        Service service = services.findByIdForUser(userId, serviceId).orElseThrow(() -> new NotFoundException("servicio"));
        return new ServiceView(service, providers.findNames(Set.of(service.getProviderId())).get(service.getProviderId()));
    }
}
