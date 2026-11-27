package org.acme.notification.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record PushEndpointRequest(@NotBlank String endpoint) {}
