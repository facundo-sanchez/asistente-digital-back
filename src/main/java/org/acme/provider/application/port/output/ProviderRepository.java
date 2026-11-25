package org.acme.provider.application.port.output;

import org.acme.provider.application.model.ProviderFilter;
import org.acme.provider.domain.model.Provider;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Proveedores visibles para un usuario: los del catálogo (userId null) más los que creó
 * él mismo. Los propios de otros usuarios nunca se devuelven (RN-002).
 */
public interface ProviderRepository {

    /** Activos y visibles, ordenados por nombre. */
    PageResult<Provider> findVisible(UUID userId, ProviderFilter filter, PageQuery page);

    Optional<Provider> findVisibleById(UUID userId, UUID providerId);

    /** Nombre de cada proveedor pedido, por id (para mostrar junto a los servicios). */
    Map<UUID, String> findNames(Collection<UUID> providerIds);
}
