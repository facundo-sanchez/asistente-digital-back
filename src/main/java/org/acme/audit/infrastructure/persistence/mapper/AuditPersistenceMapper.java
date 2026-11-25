package org.acme.audit.infrastructure.persistence.mapper;

import org.acme.audit.domain.model.AuditLog;
import org.acme.audit.infrastructure.persistence.entity.AuditLogEntity;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface AuditPersistenceMapper {
    AuditLog toModel(AuditLogEntity entity);
    AuditLogEntity toEntity(AuditLog model);
}
