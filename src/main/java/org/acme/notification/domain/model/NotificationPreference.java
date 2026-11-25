package org.acme.notification.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Preferencia de notificación por canal y tipo (CU-007, RN-009). */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {
    private UUID id;
    private UUID userId;
    private NotificationChannel channel;
    @Builder.Default
    private NotificationType notificationType = NotificationType.ALL;
    @Builder.Default
    private boolean enabled = true;
    /** Días de anticipación. 0 = el día del vencimiento. */
    @Builder.Default
    private List<Integer> daysBefore = new ArrayList<>(List.of(7, 3, 1, 0));
    private Instant createdAt;
    private Instant updatedAt;
}
