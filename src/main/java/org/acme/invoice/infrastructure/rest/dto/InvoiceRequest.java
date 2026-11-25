package org.acme.invoice.infrastructure.rest.dto;

import jakarta.validation.constraints.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Alta manual o corrección de una factura. */
@Schema(name = "InvoiceRequest")
public record InvoiceRequest(
        @NotNull(message = "El servicio es obligatorio")
        UUID serviceId,

        UUID documentId,

        @Size(max = 120)
        @Schema(example = "0001-00001234") String invoiceNumber,

        @Size(max = 30)
        @Schema(example = "2026-12") String billingPeriod,

        LocalDate issueDate,

        @NotNull(message = "La fecha de vencimiento es obligatoria")
        LocalDate dueDate,

        @NotNull(message = "El importe es obligatorio")
        @PositiveOrZero(message = "El importe no puede ser negativo")
        @Digits(integer = 15, fraction = 4)
        @Schema(example = "48500.00") BigDecimal totalAmount,

        @NotBlank(message = "La moneda es obligatoria")
        @Pattern(regexp = "[A-Z]{3}", message = "La moneda debe ser un código ISO 4217")
        @Schema(example = "ARS") String currency,

        @Size(max = 200)
        String paymentCode
) {}
