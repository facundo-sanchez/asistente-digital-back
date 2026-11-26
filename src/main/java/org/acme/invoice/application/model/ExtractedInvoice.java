package org.acme.invoice.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Factura leída por la IA (esquema invoice/1). Todo puede venir en null. */
public record ExtractedInvoice(
        String provider,
        String customerReference,
        String invoiceNumber,
        String billingPeriod,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal totalAmount,
        String currency,
        String paymentCode
) {
    public static final String SCHEMA_VERSION = "invoice/1";
}
