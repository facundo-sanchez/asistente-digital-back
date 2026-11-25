package org.acme.audit.infrastructure.rest.mapper;

import org.acme.audit.domain.model.AuditLog;
import org.acme.audit.infrastructure.rest.dto.ActivityResponse;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface AuditRestMapper {
    ActivityResponse toResponse(AuditLog auditLog);
}
