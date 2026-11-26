package org.acme.integration.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.integration.infrastructure.persistence.entity.IntegrationCredentialEntity;

import java.util.UUID;

@ApplicationScoped
public class IntegrationCredentialPanacheRepository implements PanacheRepositoryBase<IntegrationCredentialEntity, UUID> {
}
