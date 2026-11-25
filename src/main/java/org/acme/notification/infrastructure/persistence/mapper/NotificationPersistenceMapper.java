package org.acme.notification.infrastructure.persistence.mapper;

import org.acme.notification.domain.model.Notification;
import org.acme.notification.domain.model.NotificationPreference;
import org.acme.notification.infrastructure.persistence.entity.NotificationEntity;
import org.acme.notification.infrastructure.persistence.entity.NotificationPreferenceEntity;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface NotificationPersistenceMapper {
    Notification toModel(NotificationEntity entity);
    NotificationEntity toEntity(Notification model);

    NotificationPreference toModel(NotificationPreferenceEntity entity);
    NotificationPreferenceEntity toEntity(NotificationPreference model);
}
