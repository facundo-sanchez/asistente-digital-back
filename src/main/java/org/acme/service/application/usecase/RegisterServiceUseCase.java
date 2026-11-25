package org.acme.service.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.provider.domain.model.Provider;
import org.acme.service.application.model.ServiceView;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.service.domain.model.Service;

import java.util.UUID;

/** Registrar un servicio (CU-001). Queda activo y disponible para asociarle facturas. */
@ApplicationScoped
public class RegisterServiceUseCase {

    private final ServiceRepository services;
    private final ServiceDraftValidator validator;

    @Inject
    public RegisterServiceUseCase(ServiceRepository services, ServiceDraftValidator validator) {
        this.services = services;
        this.validator = validator;
    }

    /** draft trae los datos del formulario; el dueño y el estado los asigna el caso de uso. */
    @Transactional
    public ServiceView register(UUID userId, Service draft) {
        Provider provider = validator.validate(userId, draft, null);
        Service saved = services.save(validator.clean(draft).toBuilder()
                .id(null)
                .userId(userId)
                .active(true)
                .build());
        return new ServiceView(saved, provider.getName());
    }
}
