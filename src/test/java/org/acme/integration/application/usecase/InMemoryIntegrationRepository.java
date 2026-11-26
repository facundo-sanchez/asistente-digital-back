package org.acme.integration.application.usecase;

import org.acme.integration.application.port.output.IntegrationRepository;
import org.acme.integration.domain.model.Integration;
import org.acme.integration.domain.model.IntegrationProvider;
import org.acme.integration.domain.model.IntegrationStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryIntegrationRepository implements IntegrationRepository {

    public final Map<UUID, Integration> rows = new LinkedHashMap<>();

    @Override
    public Integration save(Integration integration) {
        Integration copy = integration.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
            copy.setCreatedAt(Instant.parse("2026-11-26T12:00:00Z"));
        }
        rows.put(copy.getId(), copy);
        return copy.toBuilder().build();
    }

    @Override
    public Optional<Integration> findByIdForUser(UUID userId, UUID id) {
        return Optional.ofNullable(rows.get(id)).filter(i -> i.getUserId().equals(userId)).map(i -> i.toBuilder().build());
    }

    @Override
    public List<Integration> findForUser(UUID userId) {
        return rows.values().stream().filter(i -> i.getUserId().equals(userId)).map(i -> i.toBuilder().build()).toList();
    }

    @Override
    public Optional<Integration> findByAccount(UUID userId, IntegrationProvider provider, String externalAccountId) {
        return rows.values().stream().filter(i -> i.getUserId().equals(userId) && i.getProvider() == provider
                && i.getExternalAccountId().equals(externalAccountId)).findFirst().map(i -> i.toBuilder().build());
    }

    @Override
    public List<Integration> findActive(IntegrationProvider provider) {
        return rows.values().stream().filter(i -> i.getProvider() == provider && i.getStatus() == IntegrationStatus.ACTIVE)
                .map(i -> i.toBuilder().build()).toList();
    }
}
