package org.acme.integration.application.port.output;

import org.acme.integration.domain.model.IntegrationCredential;

import java.util.Optional;
import java.util.UUID;

public interface IntegrationCredentialRepository {

    Optional<IntegrationCredential> findByIntegration(UUID integrationId);

    /** Una por integración: reemplaza la que haya. */
    IntegrationCredential save(IntegrationCredential credential);

    void deleteByIntegration(UUID integrationId);
}
