package org.acme.provider.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.provider.application.model.ProviderFilter;
import org.acme.provider.application.port.output.ProviderRepository;
import org.acme.provider.domain.model.Provider;
import org.acme.shared.application.SearchText;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;
import org.acme.shared.domain.model.ServiceCategory;

import java.util.UUID;

/** Consulta del catálogo de proveedores visible para el usuario. */
@ApplicationScoped
public class ProviderQueryUseCase {

    private final ProviderRepository providers;

    @Inject
    public ProviderQueryUseCase(ProviderRepository providers) {
        this.providers = providers;
    }

    public PageResult<Provider> list(UUID userId, String text, ServiceCategory category, PageQuery page) {
        return providers.findVisible(userId, new ProviderFilter(SearchText.normalize(text), category), page);
    }

    /** Un proveedor propio de otro usuario responde igual que uno inexistente: no se revela que existe. */
    public Provider get(UUID userId, UUID providerId) {
        return providers.findVisibleById(userId, providerId).orElseThrow(() -> new NotFoundException("proveedor"));
    }
}
