package org.acme.integration.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Aviso de cuota leído del email del banco: "La cuota 10 de su préstamo personal 203/2, por ars 584292,14, vence el 07/11/2026." */
public record LoanNotice(int installmentNumber, String reference, String currency, BigDecimal amount, LocalDate dueDate) {}
