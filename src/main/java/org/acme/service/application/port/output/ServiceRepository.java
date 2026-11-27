package org.acme.service.application.port.output;

import org.acme.service.application.model.ServiceFilter;
import org.acme.service.domain.model.Service;
import org.acme.shared.domain.model.ServiceCategory;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Servicios de un usuario. Toda consulta va filtrada por userId (RN-002). */
public interface ServiceRepository {

    /** Inserta si no tiene id; si no, actualiza. Devuelve el servicio persistido. */
    Service save(Service service);

    Optional<Service> findByIdForUser(UUID userId, UUID serviceId);

    /** Ids de los servicios activos del usuario que se pagan por débito automático. */
    Set<UUID> findAutoDebitIds(UUID userId);

    /** Todos los servicios del usuario con ese proveedor (para detectar duplicados). */
    List<Service> findByProvider(UUID userId, UUID providerId);

    /** Nombre de cada servicio pedido del usuario, por id (para mostrar junto a otros datos). */
    Map<UUID, String> findNames(UUID userId, Collection<UUID> serviceIds);

    /** Categoría de cada servicio del usuario, para el color en las listas. */
    Map<UUID, ServiceCategory> findCategories(UUID userId, Collection<UUID> serviceIds);

    /** Ordenados por nombre. */
    PageResult<Service> findForUser(UUID userId, ServiceFilter filter, PageQuery page);
}
