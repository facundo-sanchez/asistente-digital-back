package org.acme.obligation.infrastructure.rest.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.acme.obligation.domain.model.ObligationType;
import org.acme.shared.domain.model.BillingFrequency;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Alta / edición de un vencimiento que se repite. nextExpectedDate es el vencimiento del próximo
 * período (al crear, el primero); su día del mes se repite en los siguientes. amount y currency van juntos.
 */
@Schema(name = "RecurringRuleRequest")
public record RecurringRuleRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "Hasta 200 caracteres")
        @Schema(example = "Alquiler depto") String title,

        @NotNull(message = "El tipo es obligatorio")
        ObligationType obligationType,

        UUID serviceId,

        @NotNull(message = "Elegí cada cuánto se repite")
        @Schema(example = "MONTHLY") BillingFrequency frequency,

        @NotNull(message = "La fecha de vencimiento es obligatoria")
        @Schema(example = "2026-12-10") LocalDate nextExpectedDate,

        @PositiveOrZero(message = "El importe no puede ser negativo")
        @Digits(integer = 15, fraction = 4)
        @Schema(example = "350000") BigDecimal amount,

        @Pattern(regexp = "[A-Z]{3}", message = "La moneda debe ser un código ISO 4217")
        @Schema(example = "ARS") String currency
) {}
