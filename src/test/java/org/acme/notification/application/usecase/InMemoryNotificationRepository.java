package org.acme.notification.application.usecase;

import org.acme.notification.application.port.output.NotificationRepository;
import org.acme.notification.domain.model.Notification;
import org.acme.notification.domain.model.NotificationChannel;
import org.acme.notification.domain.model.NotificationStatus;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class InMemoryNotificationRepository implements NotificationRepository {

    public final List<Notification> rows = new ArrayList<>();
    private Instant createdAt = Instant.parse("2026-11-20T10:00:00Z");

    @Override
    public boolean saveIfAbsent(Notification notification) {
        if (rows.stream().anyMatch(n -> n.getIdempotencyKey().equals(notification.getIdempotencyKey()))) {
            return false;
        }
        save(notification);
        return true;
    }

    @Override
    public Notification save(Notification notification) {
        Notification copy = notification.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
            copy.setCreatedAt(createdAt);
            createdAt = createdAt.plusSeconds(1);
        } else {
            rows.removeIf(n -> n.getId().equals(copy.getId()));
        }
        rows.add(copy);
        return copy.toBuilder().build();
    }

    @Override
    public Optional<Notification> findByIdForUser(UUID userId, UUID id) {
        return rows.stream().filter(n -> n.getId().equals(id) && n.getUserId().equals(userId)).findFirst().map(n -> n.toBuilder().build());
    }

    @Override
    public PageResult<Notification> findForUser(UUID userId, boolean unreadOnly, Instant since, PageQuery page) {
        List<Notification> found = rows.stream()
                .filter(n -> n.getUserId().equals(userId) && n.getChannel() == NotificationChannel.WEB && !n.getCreatedAt().isBefore(since) && (!unreadOnly || !n.isRead()))
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed()).toList();
        return new PageResult<>(found, page.page(), page.size(), found.size());
    }

    @Override
    public long countUnread(UUID userId, Instant since) {
        return findForUser(userId, true, since, new PageQuery(0, PageQuery.MAX_SIZE)).totalElements();
    }

    @Override
    public int markAllRead(UUID userId, Instant now) {
        List<Notification> unread = rows.stream().filter(n -> n.getUserId().equals(userId) && !n.isRead()).toList();
        unread.forEach(n -> n.markAsRead(now));
        return unread.size();
    }

    @Override
    public List<Notification> findDueEmails(Instant now, int limit) {
        return rows.stream().filter(n -> n.getChannel() == NotificationChannel.EMAIL && n.getStatus() == NotificationStatus.PENDING
                        && !n.getScheduledAt().isAfter(now))
                .sorted(Comparator.comparing(Notification::getScheduledAt)).limit(limit).map(n -> n.toBuilder().build()).toList();
    }
}
