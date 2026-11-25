package org.acme.card.infrastructure.rest.dto;

import org.acme.card.domain.model.SpendingCategory;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "CardMerchantRuleResponse")
public record CardMerchantRuleResponse(
        UUID id,
        @Schema(example = "SUPERMERCADO X") String merchantName,
        SpendingCategory category,
        Instant createdAt,
        Instant updatedAt
) {}
