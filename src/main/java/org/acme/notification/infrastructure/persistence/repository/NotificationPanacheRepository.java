package org.acme.notification.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.notification.infrastructure.persistence.entity.NotificationEntity;

import java.util.UUID;

@ApplicationScoped
public class NotificationPanacheRepository implements PanacheRepositoryBase<NotificationEntity, UUID> {
}
