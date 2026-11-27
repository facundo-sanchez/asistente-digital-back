package org.acme.loan.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Comprobante de alta de un préstamo leído por la IA (esquema loan_contract/1). Todo puede venir en null. */
public record ExtractedLoanContract(
        String lender,
        String loanNumber,
        String loanType,
        BigDecimal principal,
        String currency,
        String rateType,
        BigDecimal annualRate,
        LocalDate disbursementDate
) {
    public static final String SCHEMA_VERSION = "loan_contract/1";
}
