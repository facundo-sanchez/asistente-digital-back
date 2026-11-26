package org.acme.notification.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.notification.application.port.output.NotificationRepository;
import org.acme.notification.domain.model.Notification;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** La campana: los avisos en la app de los últimos 30 días. */
@ApplicationScoped
public class NotificationQueryUseCase {

    static final Duration WINDOW = Duration.ofDays(30);

    private final NotificationRepository notifications;
    private final Clock clock;

    @Inject
    public NotificationQueryUseCase(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    public PageResult<Notification> list(UUID userId, boolean unreadOnly, PageQuery page) {
        return notifications.findForUser(userId, unreadOnly, since(), page);
    }

    public long unreadCount(UUID userId) {
        return notifications.countUnread(userId, since());
    }

    public Notification markRead(UUID userId, UUID id) {
        Notification notification = notifications.findByIdForUser(userId, id).orElseThrow(() -> new NotFoundException("aviso"));
        if (notification.isRead()) {
            return notification;
        }
        notification.markAsRead(Instant.now(clock));
        return notifications.save(notification);
    }

    public int markAllRead(UUID userId) {
        return notifications.markAllRead(userId, Instant.now(clock));
    }

    private Instant since() {
        return Instant.now(clock).minus(WINDOW);
    }
}
