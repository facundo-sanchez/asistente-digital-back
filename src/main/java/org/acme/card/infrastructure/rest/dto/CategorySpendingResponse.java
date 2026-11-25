package org.acme.card.infrastructure.rest.dto;

import org.acme.card.domain.model.SpendingCategory;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "CategorySpendingResponse")
public record CategorySpendingResponse(
        SpendingCategory category,
        String currency,
        BigDecimal total,
        long count
) {}
