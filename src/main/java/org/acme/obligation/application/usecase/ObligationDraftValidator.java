package org.acme.obligation.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exceptions.ServiceException;
import org.acme.obligation.domain.model.Obligation;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.service.domain.model.Service;

import java.util.Set;
import java.util.UUID;

/** Reglas comunes al alta y a la edición de una obligación. */
@ApplicationScoped
public class ObligationDraftValidator {

    static final String UNKNOWN_SERVICE = "El servicio elegido no existe";
    static final String INVALID_CURRENCY = "La moneda tiene que ser ARS o USD";
    static final String AMOUNT_WITHOUT_CURRENCY = "Importe y moneda van juntos";

    private static final Set<String> CURRENCIES = Set.of("ARS", "USD");

    private final ServiceRepository services;

    @Inject
    public ObligationDraftValidator(ServiceRepository services) {
        this.services = services;
    }

    /** Valida el borrador y devuelve el nombre de su servicio (null si no tiene). */
    public String validate(UUID userId, Obligation draft) {
        if ((draft.getAmount() == null) != (draft.getCurrency() == null)) {
            throw new ServiceException(AMOUNT_WITHOUT_CURRENCY);
        }
        if (draft.getCurrency() != null && !CURRENCIES.contains(draft.getCurrency())) {
            throw new ServiceException(INVALID_CURRENCY);
        }
        if (draft.getServiceId() == null) {
            return null;
        }
        // Un servicio de otro usuario responde igual que uno inexistente (RN-002)
        return services.findByIdForUser(userId, draft.getServiceId())
                .map(Service::getName)
                .orElseThrow(() -> new ServiceException(UNKNOWN_SERVICE));
    }

    /** Sin espacios de más; un texto vacío se guarda como null. */
    public static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
