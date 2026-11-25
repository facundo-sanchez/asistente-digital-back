package org.acme.notification.infrastructure.rest.dto;

import org.acme.notification.domain.model.NotificationChannel;
import org.acme.notification.domain.model.NotificationType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "NotificationResponse")
public record NotificationResponse(
        UUID id,
        UUID obligationId,
        UUID documentId,
        UUID integrationId,
        NotificationType notificationType,
        NotificationChannel channel,
        @Schema(example = "Tu factura de luz vence en 3 días") String title,
        String message,
        boolean read,
        Instant readAt,
        Instant createdAt
) {}
