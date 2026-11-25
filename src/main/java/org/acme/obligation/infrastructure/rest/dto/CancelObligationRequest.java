package org.acme.obligation.infrastructure.rest.dto;

import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CancelObligationRequest")
public record CancelObligationRequest(
        @Size(max = 500)
        @Schema(example = "Se dio de baja el servicio") String reason
) {}
