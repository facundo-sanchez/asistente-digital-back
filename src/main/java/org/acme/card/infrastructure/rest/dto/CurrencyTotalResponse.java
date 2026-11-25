package org.acme.card.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "CurrencyTotalResponse")
public record CurrencyTotalResponse(
        @Schema(example = "ARS") String currency,
        @Schema(example = "1850000.00") BigDecimal total,
        @Schema(example = "142") long count
) {}
