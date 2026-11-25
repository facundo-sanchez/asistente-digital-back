package org.acme.service.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.ServiceException;
import org.acme.provider.application.port.output.ProviderRepository;
import org.acme.provider.domain.model.Provider;
import org.acme.service.application.model.ServiceView;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.service.domain.model.Service;

import java.util.HashMap;
import java.util.UUID;

/** Registrar un servicio (CU-001). Queda activo y disponible para asociarle facturas. */
@ApplicationScoped
public class RegisterServiceUseCase {

    static final String UNKNOWN_PROVIDER = "El proveedor elegido no existe";
    static final String INACTIVE_PROVIDER = "El proveedor elegido ya no está disponible";
    static final String INVALID_DATES = "La fecha de fin no puede ser anterior a la de inicio";

    private final ServiceRepository services;
    private final ProviderRepository providers;

    @Inject
    public RegisterServiceUseCase(ServiceRepository services, ProviderRepository providers) {
        this.services = services;
        this.providers = providers;
    }

    /** draft trae los datos del formulario; el dueño y el estado los asigna el caso de uso. */
    @Transactional
    public ServiceView register(UUID userId, Service draft) {
        // Un proveedor propio de otro usuario se trata como inexistente (RN-002)
        Provider provider = providers.findVisibleById(userId, draft.getProviderId())
                .orElseThrow(() -> new ServiceException(UNKNOWN_PROVIDER));
        if (!provider.isActive()) {
            throw new ServiceException(INACTIVE_PROVIDER);
        }
        if (draft.getStartDate() != null && draft.getEndDate() != null && draft.getEndDate().isBefore(draft.getStartDate())) {
            throw new ServiceException(INVALID_DATES);
        }

        Service saved = services.save(draft.toBuilder()
                .id(null)
                .userId(userId)
                .name(draft.getName().trim())
                .customerReference(blankToNull(draft.getCustomerReference()))
                .notes(blankToNull(draft.getNotes()))
                .metadata(draft.getMetadata() == null ? new HashMap<>() : draft.getMetadata())
                .active(true)
                .build());
        return new ServiceView(saved, provider.getName());
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
