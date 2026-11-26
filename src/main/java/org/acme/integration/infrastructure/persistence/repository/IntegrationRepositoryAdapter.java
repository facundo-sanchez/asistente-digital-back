package org.acme.integration.infrastructure.persistence.repository;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.integration.application.port.output.IntegrationRepository;
import org.acme.integration.domain.model.Integration;
import org.acme.integration.domain.model.IntegrationProvider;
import org.acme.integration.domain.model.IntegrationStatus;
import org.acme.integration.infrastructure.persistence.entity.IntegrationEntity;
import org.acme.integration.infrastructure.persistence.mapper.IntegrationPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class IntegrationRepositoryAdapter implements IntegrationRepository {

    @Inject
    IntegrationPanacheRepository repository;

    @Inject
    IntegrationPersistenceMapper mapper;

    @Override
    @Transactional
    public Integration save(Integration integration) {
        IntegrationEntity entity = mapper.toEntity(integration);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    public Optional<Integration> findByIdForUser(UUID userId, UUID id) {
        return repository.find("id = ?1 and userId = ?2", id, userId).firstResultOptional().map(mapper::toModel);
    }

    @Override
    public List<Integration> findForUser(UUID userId) {
        return repository.list("userId", Sort.by("createdAt"), userId).stream().map(mapper::toModel).toList();
    }

    @Override
    public Optional<Integration> findByAccount(UUID userId, IntegrationProvider provider, String externalAccountId) {
        return repository.find("userId = ?1 and provider = ?2 and externalAccountId = ?3", userId, provider, externalAccountId)
                .firstResultOptional().map(mapper::toModel);
    }

    @Override
    public List<Integration> findActive(IntegrationProvider provider) {
        return repository.list("provider = ?1 and status = ?2", provider, IntegrationStatus.ACTIVE).stream().map(mapper::toModel).toList();
    }
}
