package org.acme.loan.infrastructure.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LoanReceiptConfirmationRequest(
        @NotNull(message = "Elegí el préstamo") UUID loanId,
        @Min(value = 1, message = "Poné el número de cuota") int installmentNumber,
        @NotNull(message = "Poné el importe pagado") @Positive BigDecimal amount,
        @NotNull(message = "Poné la fecha de pago") LocalDate paymentDate,
        BigDecimal newBalance
) {}
