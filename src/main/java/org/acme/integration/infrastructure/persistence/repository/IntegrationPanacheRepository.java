package org.acme.integration.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.integration.infrastructure.persistence.entity.IntegrationEntity;

import java.util.UUID;

@ApplicationScoped
public class IntegrationPanacheRepository implements PanacheRepositoryBase<IntegrationEntity, UUID> {
}
