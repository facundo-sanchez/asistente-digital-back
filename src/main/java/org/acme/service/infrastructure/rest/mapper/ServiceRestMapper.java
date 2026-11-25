package org.acme.service.infrastructure.rest.mapper;

import org.acme.service.domain.model.Service;
import org.acme.service.infrastructure.rest.dto.ServiceRequest;
import org.acme.service.infrastructure.rest.dto.ServiceResponse;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ServiceRestMapper {

    ServiceResponse toResponse(Service service);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Service toModel(ServiceRequest request);

    @InheritConfiguration(name = "toModel")
    void update(ServiceRequest request, @MappingTarget Service service);
}
