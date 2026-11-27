package org.acme.loan.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lo que el usuario confirma del comprobante de pago (puede corregir lo que leyó la IA). */
public record LoanReceiptConfirmation(java.util.UUID loanId, int installmentNumber, BigDecimal amount, LocalDate paymentDate, BigDecimal newBalance) {}
