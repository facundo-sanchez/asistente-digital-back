package org.acme.notification.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushSubscriptionKeys(@NotBlank @Size(max = 200) String p256dh, @NotBlank @Size(max = 100) String auth) {}
