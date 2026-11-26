package org.acme.notification.application.port.output;

import org.acme.notification.domain.model.Notification;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    /** Guarda el aviso si no hay otro con su idempotencyKey. Devuelve true si lo guardó. */
    boolean saveIfAbsent(Notification notification);

    Notification save(Notification notification);

    Optional<Notification> findByIdForUser(UUID userId, UUID id);

    /** Avisos en la app creados desde since, los más nuevos primero. */
    PageResult<Notification> findForUser(UUID userId, boolean unreadOnly, Instant since, PageQuery page);

    long countUnread(UUID userId, Instant since);

    /** Marca leídos todos los avisos en la app del usuario. Devuelve cuántos cambió. */
    int markAllRead(UUID userId, Instant now);

    /** Emails pendientes con scheduledAt ya cumplido, los más viejos primero. */
    List<Notification> findDueEmails(Instant now, int limit);
}
