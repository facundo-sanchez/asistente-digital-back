package org.acme.service.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ServiceStatusRequest")
public record ServiceStatusRequest(
        @NotNull(message = "El estado es obligatorio")
        Boolean active
) {}
