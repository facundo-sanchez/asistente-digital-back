package org.acme.payment.infrastructure.rest.dto;

import jakarta.validation.constraints.*;
import org.acme.payment.domain.model.PaymentMethod;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Registrar un pago. PUT /payments/{id} usa el mismo body: anula el pago y crea uno nuevo que lo reemplaza. */
@Schema(name = "PaymentRequest")
public record PaymentRequest(
        @NotNull(message = "La obligación es obligatoria")
        UUID obligationId,

        @NotNull(message = "El importe es obligatorio")
        @Positive(message = "El importe debe ser mayor a cero")
        @Digits(integer = 15, fraction = 4)
        @Schema(example = "20000.00") BigDecimal amount,

        @NotBlank(message = "La moneda es obligatoria")
        @Pattern(regexp = "[A-Z]{3}", message = "La moneda debe ser un código ISO 4217")
        @Schema(example = "ARS") String currency,

        @NotNull(message = "La fecha de pago es obligatoria")
        @PastOrPresent(message = "La fecha de pago no puede ser futura")
        LocalDate paymentDate,

        @NotNull(message = "El medio de pago es obligatorio")
        PaymentMethod paymentMethod,

        UUID receiptDocumentId,

        @Size(max = 1000)
        String notes
) {}
