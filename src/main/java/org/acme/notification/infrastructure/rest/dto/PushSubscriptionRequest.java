package org.acme.notification.infrastructure.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Lo que devuelve PushManager.subscribe() en el navegador (toJSON), más el navegador. */
@Schema(name = "PushSubscriptionRequest")
public record PushSubscriptionRequest(
        @NotBlank @Size(max = 2000) String endpoint,
        @NotNull @Valid PushSubscriptionKeys keys,
        @Size(max = 300) String userAgent
) {}
