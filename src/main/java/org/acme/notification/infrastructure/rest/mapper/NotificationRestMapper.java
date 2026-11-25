package org.acme.notification.infrastructure.rest.mapper;

import org.acme.notification.domain.model.Notification;
import org.acme.notification.domain.model.NotificationPreference;
import org.acme.notification.infrastructure.rest.dto.NotificationPreferenceRequest;
import org.acme.notification.infrastructure.rest.dto.NotificationPreferenceResponse;
import org.acme.notification.infrastructure.rest.dto.NotificationResponse;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface NotificationRestMapper {

    NotificationResponse toResponse(Notification notification);

    NotificationPreferenceResponse toResponse(NotificationPreference preference);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    NotificationPreference toModel(NotificationPreferenceRequest request);
}
