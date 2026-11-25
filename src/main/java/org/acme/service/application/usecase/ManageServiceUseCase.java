package org.acme.service.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.provider.application.port.output.ProviderRepository;
import org.acme.provider.domain.model.Provider;
import org.acme.service.application.model.ServiceView;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.service.domain.model.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Mantenimiento de un servicio: edición, activar/desactivar y baja. La baja es lógica
 * (desactiva): el historial de facturas y obligaciones se conserva.
 */
@ApplicationScoped
public class ManageServiceUseCase {

    private final ServiceRepository services;
    private final ProviderRepository providers;
    private final ServiceDraftValidator validator;

    @Inject
    public ManageServiceUseCase(ServiceRepository services, ProviderRepository providers, ServiceDraftValidator validator) {
        this.services = services;
        this.providers = providers;
        this.validator = validator;
    }

    /** Reemplaza los datos editables. Dueño, estado y fecha de alta no cambian. */
    @Transactional
    public ServiceView update(UUID userId, UUID serviceId, Service draft) {
        Service current = find(userId, serviceId);
        Provider provider = validator.validate(userId, draft, current.getProviderId());
        Service saved = services.save(validator.clean(draft).toBuilder()
                .id(current.getId())
                .userId(current.getUserId())
                .active(current.isActive())
                .createdAt(current.getCreatedAt())
                .build());
        return new ServiceView(saved, provider.getName());
    }

    @Transactional
    public ServiceView changeStatus(UUID userId, UUID serviceId, boolean active) {
        Service service = find(userId, serviceId);
        service.setActive(active);
        return view(services.save(service));
    }

    /** Baja lógica: equivale a desactivar. Idempotente. */
    @Transactional
    public void delete(UUID userId, UUID serviceId) {
        changeStatus(userId, serviceId, false);
    }

    private Service find(UUID userId, UUID serviceId) {
        return services.findByIdForUser(userId, serviceId).orElseThrow(() -> new NotFoundException("servicio"));
    }

    private ServiceView view(Service service) {
        return new ServiceView(service, providers.findNames(Set.of(service.getProviderId())).get(service.getProviderId()));
    }
}
