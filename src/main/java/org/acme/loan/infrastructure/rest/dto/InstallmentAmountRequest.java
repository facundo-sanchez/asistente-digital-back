package org.acme.loan.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record InstallmentAmountRequest(@NotNull(message = "Poné el importe") @Positive(message = "El importe tiene que ser mayor a cero") BigDecimal amount) {}
