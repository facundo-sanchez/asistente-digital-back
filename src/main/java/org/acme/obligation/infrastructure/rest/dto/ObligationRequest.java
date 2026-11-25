package org.acme.obligation.infrastructure.rest.dto;

import jakarta.validation.constraints.*;
import org.acme.obligation.domain.model.ObligationType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Alta / edición manual de una obligación (ej. VTV, impuesto).
 * Las obligaciones de facturas las genera el backend al confirmar la factura.
 * amount y currency van juntos: ambos o ninguno.
 */
@Schema(name = "ObligationRequest")
public record ObligationRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200)
        @Schema(example = "VTV auto") String title,

        String description,

        @NotNull(message = "El tipo es obligatorio")
        ObligationType obligationType,

        UUID serviceId,
        UUID assetId,

        @PositiveOrZero(message = "El importe no puede ser negativo")
        @Digits(integer = 15, fraction = 4)
        BigDecimal amount,

        @Pattern(regexp = "[A-Z]{3}", message = "La moneda debe ser un código ISO 4217")
        String currency,

        @NotNull(message = "La fecha de vencimiento es obligatoria")
        LocalDate dueDate
) {
    @AssertTrue(message = "Importe y moneda deben informarse juntos")
    private boolean isAmountWithCurrency() {
        return (amount == null) == (currency == null);
    }
}
