package org.acme.loan.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Comprobante de pago de una cuota leído por la IA (esquema loan_receipt/1). Todo puede venir en null. */
public record ExtractedLoanReceipt(
        String lender,
        String loanNumber,
        BigDecimal principal,
        LocalDate disbursementDate,
        Integer installmentNumber,
        Integer pendingInstallments,
        LocalDate dueDate,
        LocalDate paymentDate,
        BigDecimal totalAmount,
        String currency,
        BigDecimal capital,
        BigDecimal interest,
        BigDecimal vat,
        BigDecimal stamps,
        BigDecimal previousBalance,
        BigDecimal newBalance
) {
    public static final String SCHEMA_VERSION = "loan_receipt/1";
}
