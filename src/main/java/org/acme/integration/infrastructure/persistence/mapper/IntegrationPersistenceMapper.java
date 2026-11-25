package org.acme.integration.infrastructure.persistence.mapper;

import org.acme.integration.domain.model.Integration;
import org.acme.integration.domain.model.IntegrationCredential;
import org.acme.integration.domain.model.IntegrationProcessedItem;
import org.acme.integration.infrastructure.persistence.entity.IntegrationCredentialEntity;
import org.acme.integration.infrastructure.persistence.entity.IntegrationEntity;
import org.acme.integration.infrastructure.persistence.entity.IntegrationProcessedItemEntity;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface IntegrationPersistenceMapper {
    Integration toModel(IntegrationEntity entity);
    IntegrationEntity toEntity(Integration model);

    IntegrationCredential toModel(IntegrationCredentialEntity entity);
    IntegrationCredentialEntity toEntity(IntegrationCredential model);

    IntegrationProcessedItem toModel(IntegrationProcessedItemEntity entity);
    IntegrationProcessedItemEntity toEntity(IntegrationProcessedItem model);
}
