package org.acme.integration.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.integration.infrastructure.persistence.entity.IntegrationProcessedItemEntity;

import java.util.UUID;

@ApplicationScoped
public class ProcessedItemPanacheRepository implements PanacheRepositoryBase<IntegrationProcessedItemEntity, UUID> {
}
