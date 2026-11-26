package org.acme.integration.infrastructure.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.integration.application.port.output.IntegrationCredentialRepository;
import org.acme.integration.domain.model.IntegrationCredential;
import org.acme.integration.infrastructure.persistence.entity.IntegrationCredentialEntity;
import org.acme.integration.infrastructure.persistence.mapper.IntegrationPersistenceMapper;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class IntegrationCredentialRepositoryAdapter implements IntegrationCredentialRepository {

    @Inject
    IntegrationCredentialPanacheRepository repository;

    @Inject
    IntegrationPersistenceMapper mapper;

    @Override
    public Optional<IntegrationCredential> findByIntegration(UUID integrationId) {
        return repository.find("integrationId", integrationId).firstResultOptional().map(mapper::toModel);
    }

    @Override
    @Transactional
    public IntegrationCredential save(IntegrationCredential credential) {
        IntegrationCredentialEntity entity = mapper.toEntity(credential);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    @Transactional
    public void deleteByIntegration(UUID integrationId) {
        repository.delete("integrationId", integrationId);
    }
}
