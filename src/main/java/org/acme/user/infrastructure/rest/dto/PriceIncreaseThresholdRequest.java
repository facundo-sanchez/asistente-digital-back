package org.acme.user.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "PriceIncreaseThresholdRequest")
public record PriceIncreaseThresholdRequest(
        @Schema(description = "10, 20, 30 o 50", example = "20") int threshold
) {}
