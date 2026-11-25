package org.acme.asset.infrastructure.persistence.mapper;

import org.acme.asset.domain.model.Asset;
import org.acme.asset.infrastructure.persistence.entity.AssetEntity;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface AssetPersistenceMapper {
    Asset toModel(AssetEntity entity);
    AssetEntity toEntity(Asset model);
}
