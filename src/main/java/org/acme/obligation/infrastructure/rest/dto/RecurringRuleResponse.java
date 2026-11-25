package org.acme.obligation.infrastructure.rest.dto;

import org.acme.obligation.domain.model.RecurrenceOrigin;
import org.acme.shared.domain.model.BillingFrequency;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "RecurringRuleResponse")
public record RecurringRuleResponse(
        UUID id,
        UUID serviceId,
        UUID assetId,
        @Schema(example = "Luz mensual") String title,
        BillingFrequency frequency,
        Integer dayOfMonth,
        LocalDate nextExpectedDate,
        BigDecimal estimatedAmount,
        String currency,
        RecurrenceOrigin origin,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
