package org.acme.service.infrastructure.persistence.mapper;

import org.acme.service.domain.model.Service;
import org.acme.service.infrastructure.persistence.entity.ServiceEntity;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface ServicePersistenceMapper {
    Service toModel(ServiceEntity entity);
    ServiceEntity toEntity(Service model);
}
