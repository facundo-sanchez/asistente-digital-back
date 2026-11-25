package org.acme.notification.infrastructure.rest.dto;

import org.acme.notification.domain.model.NotificationChannel;
import org.acme.notification.domain.model.NotificationType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "NotificationPreferenceResponse")
public record NotificationPreferenceResponse(
        UUID id,
        NotificationChannel channel,
        NotificationType notificationType,
        boolean enabled,
        @Schema(description = "Días de anticipación; 0 = el día del vencimiento", example = "[7, 3, 1, 0]")
        List<Integer> daysBefore
) {}
