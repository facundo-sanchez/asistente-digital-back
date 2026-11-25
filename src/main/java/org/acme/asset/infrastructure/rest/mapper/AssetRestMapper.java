package org.acme.asset.infrastructure.rest.mapper;

import org.acme.asset.domain.model.Asset;
import org.acme.asset.infrastructure.rest.dto.AssetRequest;
import org.acme.asset.infrastructure.rest.dto.AssetResponse;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface AssetRestMapper {

    AssetResponse toResponse(Asset asset);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Asset toModel(AssetRequest request);

    @InheritConfiguration(name = "toModel")
    void update(AssetRequest request, @MappingTarget Asset asset);
}
