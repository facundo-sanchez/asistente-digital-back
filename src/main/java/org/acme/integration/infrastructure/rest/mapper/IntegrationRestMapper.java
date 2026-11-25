package org.acme.integration.infrastructure.rest.mapper;

import org.acme.integration.domain.model.Integration;
import org.acme.integration.domain.model.IntegrationProcessedItem;
import org.acme.integration.infrastructure.rest.dto.DetectedEmailResponse;
import org.acme.integration.infrastructure.rest.dto.IntegrationResponse;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface IntegrationRestMapper {

    IntegrationResponse toResponse(Integration integration);

    DetectedEmailResponse toResponse(IntegrationProcessedItem item);
}
