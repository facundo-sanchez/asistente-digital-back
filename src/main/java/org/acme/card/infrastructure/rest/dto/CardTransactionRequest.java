package org.acme.card.infrastructure.rest.dto;

import jakarta.validation.constraints.*;
import org.acme.card.domain.model.CardTransactionType;
import org.acme.card.domain.model.SpendingCategory;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Alta manual o edición de un consumo de un resumen. */
@Schema(name = "CardTransactionRequest")
public record CardTransactionRequest(
        @NotNull(message = "La fecha es obligatoria")
        LocalDate transactionDate,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255)
        String description,

        @Size(max = 150)
        String merchantName,

        @NotNull(message = "La categoría es obligatoria")
        SpendingCategory category,

        @NotNull(message = "El tipo es obligatorio")
        CardTransactionType transactionType,

        @NotNull(message = "El importe es obligatorio")
        @Digits(integer = 15, fraction = 4)
        @Schema(description = "Con signo: + cargo, - crédito (devoluciones y pagos son negativos)", example = "18000.00")
        BigDecimal amount,

        @NotBlank(message = "La moneda es obligatoria")
        @Pattern(regexp = "[A-Z]{3}", message = "La moneda debe ser un código ISO 4217")
        String currency,

        @Min(1) @Max(99)
        Integer installmentNumber,

        @Min(1) @Max(99)
        Integer installmentTotal,

        @Positive
        @Digits(integer = 15, fraction = 4)
        BigDecimal originalAmount,

        @Size(max = 100)
        String reference
) {
    @AssertTrue(message = "El importe no puede ser cero")
    private boolean isNonZeroAmount() {
        return amount == null || amount.signum() != 0;
    }

    @AssertTrue(message = "Las devoluciones y los pagos deben tener importe negativo")
    private boolean isCreditSignValid() {
        boolean credit = transactionType == CardTransactionType.REFUND || transactionType == CardTransactionType.PAYMENT;
        return !credit || amount == null || amount.signum() < 0;
    }

    @AssertTrue(message = "La cuota debe informarse como N de M, con N <= M")
    private boolean isInstallmentValid() {
        if (installmentNumber == null && installmentTotal == null) return true;
        return installmentNumber != null && installmentTotal != null && installmentNumber <= installmentTotal;
    }
}
