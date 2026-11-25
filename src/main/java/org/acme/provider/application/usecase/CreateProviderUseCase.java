package org.acme.provider.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.ConflictException;
import org.acme.provider.application.port.output.ProviderRepository;
import org.acme.provider.domain.model.Provider;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

/**
 * Alta de un proveedor propio, para cuando el del usuario no está en el catálogo.
 * Solo lo ve su dueño.
 */
@ApplicationScoped
public class CreateProviderUseCase {

    static final String DUPLICATE_NAME = "Ya existe un proveedor con ese nombre: elegilo de la lista";
    private static final int MAX_CODE_LENGTH = 80;

    private final ProviderRepository providers;

    @Inject
    public CreateProviderUseCase(ProviderRepository providers) {
        this.providers = providers;
    }

    /** draft trae nombre, categoría y sitio web; el dueño, el code y el estado los asigna el caso de uso. */
    @Transactional
    public Provider create(UUID userId, Provider draft) {
        String name = draft.getName().trim().replaceAll("\\s+", " ");
        if (providers.existsVisibleWithName(userId, name)) {
            throw new ConflictException(DUPLICATE_NAME);
        }
        String website = draft.getWebsite() == null || draft.getWebsite().isBlank() ? null : draft.getWebsite().trim();
        return providers.save(Provider.builder()
                .userId(userId)
                .name(name)
                .code(uniqueCode(userId, name))
                .category(draft.getCategory())
                .website(website)
                .active(true)
                .build());
    }

    /** Code legible y único entre los proveedores del usuario: "Agua Ñandú" → "AGUA_NANDU", "AGUA_NANDU_2"… */
    private String uniqueCode(UUID userId, String name) {
        String base = toCode(name);
        String code = base;
        for (int i = 2; providers.existsOwnCode(userId, code); i++) {
            String suffix = "_" + i;
            code = base.substring(0, Math.min(base.length(), MAX_CODE_LENGTH - suffix.length())) + suffix;
        }
        return code;
    }

    static String toCode(String name) {
        String ascii = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String code = ascii.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (code.isEmpty()) {
            code = "PROVEEDOR";
        }
        return code.length() > MAX_CODE_LENGTH ? code.substring(0, MAX_CODE_LENGTH) : code;
    }
}
