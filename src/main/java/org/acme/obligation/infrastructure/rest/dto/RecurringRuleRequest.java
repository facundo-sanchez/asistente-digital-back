package org.acme.obligation.infrastructure.rest.dto;

import jakarta.validation.constraints.*;
import org.acme.shared.domain.model.BillingFrequency;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Alta / edición de una regla, o confirmación de una regla detectada. */
@Schema(name = "RecurringRuleRequest")
public record RecurringRuleRequest(
        UUID serviceId,
        UUID assetId,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200)
        String title,

        @NotNull(message = "La periodicidad es obligatoria")
        BillingFrequency frequency,

        @Min(1) @Max(31)
        Integer dayOfMonth,

        @NotNull(message = "La próxima fecha es obligatoria")
        LocalDate nextExpectedDate,

        @PositiveOrZero
        @Digits(integer = 15, fraction = 4)
        BigDecimal estimatedAmount,

        @Pattern(regexp = "[A-Z]{3}", message = "La moneda debe ser un código ISO 4217")
        String currency,

        Boolean active
) {
    @AssertTrue(message = "La periodicidad NONE no es válida para una recurrencia")
    private boolean isRecurringFrequency() {
        return frequency != BillingFrequency.NONE;
    }

    @AssertTrue(message = "Importe estimado y moneda deben informarse juntos")
    private boolean isAmountWithCurrency() {
        return (estimatedAmount == null) == (currency == null);
    }
}
