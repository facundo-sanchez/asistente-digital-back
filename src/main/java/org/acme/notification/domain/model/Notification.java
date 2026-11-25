package org.acme.notification.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Notificación. Se persiste antes de enviarse; idempotencyKey evita duplicados. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private UUID id;
    private UUID userId;
    private UUID obligationId;
    private UUID documentId;
    private UUID integrationId;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private String title;
    private String message;
    /** Ej. OBLIGATION_DUE:{id}:{días}:{canal}. Uso interno. */
    private String idempotencyKey;
    private Instant scheduledAt;
    private Instant sentAt;
    private Instant readAt;
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;
    private int attempts;
    private String lastError;
    private Instant createdAt;

    public boolean isRead() {
        return readAt != null;
    }

    public void markAsRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }
}
