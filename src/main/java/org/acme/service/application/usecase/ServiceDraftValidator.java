package org.acme.service.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exceptions.ServiceException;
import org.acme.provider.application.port.output.ProviderRepository;
import org.acme.provider.domain.model.Provider;
import org.acme.service.domain.model.Service;

import java.util.HashMap;
import java.util.UUID;

/** Reglas comunes al alta y a la edición de un servicio. */
@ApplicationScoped
public class ServiceDraftValidator {

    static final String UNKNOWN_PROVIDER = "El proveedor elegido no existe";
    static final String INACTIVE_PROVIDER = "El proveedor elegido ya no está disponible";
    static final String INVALID_DATES = "La fecha de fin no puede ser anterior a la de inicio";

    private final ProviderRepository providers;

    @Inject
    public ServiceDraftValidator(ProviderRepository providers) {
        this.providers = providers;
    }

    /**
     * Valida el borrador y devuelve su proveedor. keepProviderId: al editar, el proveedor
     * que el servicio ya tenía se acepta aunque se haya dado de baja después.
     */
    public Provider validate(UUID userId, Service draft, UUID keepProviderId) {
        // Un proveedor propio de otro usuario se trata como inexistente (RN-002)
        Provider provider = providers.findVisibleById(userId, draft.getProviderId())
                .orElseThrow(() -> new ServiceException(UNKNOWN_PROVIDER));
        if (!provider.isActive() && !provider.getId().equals(keepProviderId)) {
            throw new ServiceException(INACTIVE_PROVIDER);
        }
        if (draft.getStartDate() != null && draft.getEndDate() != null && draft.getEndDate().isBefore(draft.getStartDate())) {
            throw new ServiceException(INVALID_DATES);
        }
        return provider;
    }

    /** Textos sin espacios de más; los vacíos se guardan como null. */
    public Service clean(Service draft) {
        return draft.toBuilder()
                .name(draft.getName().trim())
                .customerReference(blankToNull(draft.getCustomerReference()))
                .notes(blankToNull(draft.getNotes()))
                .metadata(draft.getMetadata() == null ? new HashMap<>() : draft.getMetadata())
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
