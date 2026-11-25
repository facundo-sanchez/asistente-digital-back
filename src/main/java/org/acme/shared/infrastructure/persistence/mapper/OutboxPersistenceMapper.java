package org.acme.shared.infrastructure.persistence.mapper;

import org.acme.shared.domain.model.OutboxEvent;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.acme.shared.infrastructure.persistence.entity.OutboxEventEntity;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface OutboxPersistenceMapper {
    OutboxEvent toModel(OutboxEventEntity entity);
    OutboxEventEntity toEntity(OutboxEvent model);
}
