package org.acme.loan.application.model;

import org.acme.loan.domain.model.Loan;

import java.math.BigDecimal;

/** El alta: el préstamo, el importe de cada cuota y cuántas se pagaron antes de cargarlo. */
public record NewLoan(Loan loan, BigDecimal installmentAmount, int paidInstallments) {}
