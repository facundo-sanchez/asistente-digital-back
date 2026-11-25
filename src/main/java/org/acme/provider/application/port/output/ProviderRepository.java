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

    /** true si el usuario ya ve un proveedor (catálogo o propio) con ese nombre, sin distinguir mayúsculas. */
    boolean existsVisibleWithName(UUID userId, String name);

    /** true si el usuario ya tiene un proveedor propio con ese code. */
    boolean existsOwnCode(UUID userId, String code);

    /** Inserta un proveedor propio. Devuelve el proveedor persistido. */
    Provider save(Provider provider);

    /** Nombre de cada proveedor pedido, por id (para mostrar junto a los servicios). */
    Map<UUID, String> findNames(Collection<UUID> providerIds);
}
