package org.acme.service.application.port.output;

import org.acme.service.application.model.ServiceFilter;
import org.acme.service.domain.model.Service;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.util.Optional;
import java.util.UUID;

/** Servicios de un usuario. Toda consulta va filtrada por userId (RN-002). */
public interface ServiceRepository {

    /** Inserta si no tiene id; si no, actualiza. Devuelve el servicio persistido. */
    Service save(Service service);

    Optional<Service> findByIdForUser(UUID userId, UUID serviceId);

    /** Ordenados por nombre. */
    PageResult<Service> findForUser(UUID userId, ServiceFilter filter, PageQuery page);
}
