package org.acme.invoice.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Lo que el usuario confirma (o corrige) de la factura propuesta por la IA. */
public record InvoiceConfirmation(
        UUID serviceId,
        String invoiceNumber,
        String billingPeriod,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal totalAmount,
        String currency,
        String paymentCode
) {}
