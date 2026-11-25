package org.acme.notification.infrastructure.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.acme.notification.domain.model.NotificationChannel;
import org.acme.notification.domain.model.NotificationType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/** Un elemento de PUT /notification-preferences (el body es una lista que reemplaza las preferencias). */
@Schema(name = "NotificationPreferenceRequest")
public record NotificationPreferenceRequest(
        @NotNull(message = "El canal es obligatorio")
        NotificationChannel channel,

        @NotNull(message = "El tipo es obligatorio")
        NotificationType notificationType,

        @NotNull(message = "Indicar si está habilitada")
        Boolean enabled,

        @NotNull(message = "Los días de anticipación son obligatorios")
        @Size(max = 10)
        List<@NotNull @Min(0) @Max(60) Integer> daysBefore
) {}
