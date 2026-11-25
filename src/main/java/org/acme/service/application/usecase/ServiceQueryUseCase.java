package org.acme.service.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.provider.application.port.output.ProviderRepository;
import org.acme.service.application.model.ServiceFilter;
import org.acme.service.application.model.ServiceView;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.service.domain.model.CustomerReference;
import org.acme.shared.application.SearchText;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;
import org.acme.shared.domain.model.ServiceCategory;

import java.util.List;
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

    /**
     * Servicios del usuario con el mismo proveedor y el mismo nro. de socio (comparado sin
     * espacios, guiones ni mayúsculas). excludeId: el servicio que se está editando.
     * Sin nro. de socio no hay con qué comparar: lista vacía.
     */
    public List<ServiceView> possibleDuplicates(UUID userId, UUID providerId, String customerReference, UUID excludeId) {
        String reference = CustomerReference.normalize(customerReference);
        if (reference == null) {
            return List.of();
        }
        List<Service> matches = services.findByProvider(userId, providerId).stream()
                .filter(s -> !s.getId().equals(excludeId))
                .filter(s -> reference.equals(CustomerReference.normalize(s.getCustomerReference())))
                .toList();
        if (matches.isEmpty()) {
            return List.of();
        }
        String providerName = providers.findNames(Set.of(providerId)).get(providerId);
        return matches.stream().map(s -> new ServiceView(s, providerName)).toList();
    }
}
