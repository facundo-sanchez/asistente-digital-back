package org.acme.integration.application.port.output;

import org.acme.integration.domain.model.Integration;
import org.acme.integration.domain.model.IntegrationProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationRepository {

    Integration save(Integration integration);

    Optional<Integration> findByIdForUser(UUID userId, UUID id);

    List<Integration> findForUser(UUID userId);

    Optional<Integration> findByAccount(UUID userId, IntegrationProvider provider, String externalAccountId);

    /** Las activas de todos los usuarios (para la sincronización). */
    List<Integration> findActive(IntegrationProvider provider);
}
