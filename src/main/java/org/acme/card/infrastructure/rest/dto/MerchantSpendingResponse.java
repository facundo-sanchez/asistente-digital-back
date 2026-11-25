package org.acme.card.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "MerchantSpendingResponse")
public record MerchantSpendingResponse(
        @Schema(example = "SUPERMERCADO X") String merchant,
        String currency,
        BigDecimal total,
        long count
) {}
