package org.acme.notification.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Un navegador o dispositivo que recibe notificaciones push. endpoint, p256dh y auth los da el navegador. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscription {
    private UUID id;
    private UUID userId;
    private String endpoint;
    private String p256dh;
    private String auth;
    private String userAgent;
    private Instant createdAt;
}
