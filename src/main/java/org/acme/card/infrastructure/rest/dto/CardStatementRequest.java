package org.acme.card.infrastructure.rest.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Carga manual o corrección de un resumen. La moneda local es la de la tarjeta. */
@Schema(name = "CardStatementRequest")
public record CardStatementRequest(
        @NotNull(message = "La tarjeta es obligatoria")
        UUID cardId,

        UUID documentId,

        LocalDate periodStart,

        @Schema(description = "Opcional si no se conoce (ej. resumen detectado por email sin PDF)")
        LocalDate closingDate,

        @NotNull(message = "La fecha de vencimiento es obligatoria")
        LocalDate dueDate,

        LocalDate nextClosingDate,
        LocalDate nextDueDate,

        @NotNull(message = "El total es obligatorio")
        @PositiveOrZero
        @Digits(integer = 15, fraction = 4)
        @Schema(example = "315000.00") BigDecimal totalAmount,

        @PositiveOrZero
        @Digits(integer = 15, fraction = 4)
        @Schema(description = "Total en dólares; por defecto 0", example = "12.99") BigDecimal totalAmountUsd,

        @PositiveOrZero
        @Digits(integer = 15, fraction = 4)
        BigDecimal minimumPayment
) {
    @AssertTrue(message = "El vencimiento no puede ser anterior al cierre")
    private boolean isDueAfterClosing() {
        return closingDate == null || dueDate == null || !dueDate.isBefore(closingDate);
    }
}
