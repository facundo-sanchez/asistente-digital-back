package org.acme.obligation.infrastructure.rest.dto;

import org.acme.obligation.domain.model.ObligationType;
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
        @Schema(example = "Alquiler depto") String title,
        ObligationType obligationType,
        BillingFrequency frequency,
        @Schema(description = "Día del mes en que vence (null si es semanal)", example = "10") Integer dayOfMonth,
        @Schema(description = "Vencimiento del próximo período que todavía no se generó") LocalDate nextExpectedDate,
        @Schema(example = "350000") BigDecimal amount,
        @Schema(example = "ARS") String currency,
        RecurrenceOrigin origin,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
