package org.acme.integration.application.usecase;

import org.acme.integration.application.port.output.IntegrationCredentialRepository;
import org.acme.integration.domain.model.IntegrationCredential;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryCredentialRepository implements IntegrationCredentialRepository {

    public final Map<UUID, IntegrationCredential> rows = new HashMap<>();

    @Override
    public Optional<IntegrationCredential> findByIntegration(UUID integrationId) {
        return Optional.ofNullable(rows.get(integrationId)).map(c -> c.toBuilder().build());
    }

    @Override
    public IntegrationCredential save(IntegrationCredential credential) {
        rows.put(credential.getIntegrationId(), credential.toBuilder().build());
        return credential;
    }

    @Override
    public void deleteByIntegration(UUID integrationId) {
        rows.remove(integrationId);
    }
}
