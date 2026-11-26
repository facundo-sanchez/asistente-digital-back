package org.acme.card.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Lo que el usuario confirma (o corrige) de la propuesta. Los movimientos se toman de la propuesta. */
public record StatementConfirmation(
        UUID cardId,
        LocalDate closingDate,
        LocalDate dueDate,
        LocalDate nextClosingDate,
        LocalDate nextDueDate,
        BigDecimal totalAmount,
        BigDecimal totalAmountUsd,
        BigDecimal minimumPayment
) {}
