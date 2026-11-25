package org.acme.provider.infrastructure.rest.mapper;

import org.acme.provider.domain.model.Provider;
import org.acme.provider.infrastructure.rest.dto.ProviderRequest;
import org.acme.provider.infrastructure.rest.dto.ProviderResponse;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface ProviderRestMapper {

    ProviderResponse toResponse(Provider provider);

    /** El code y el userId los asigna el caso de uso. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "emailDomains", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Provider toModel(ProviderRequest request);
}
