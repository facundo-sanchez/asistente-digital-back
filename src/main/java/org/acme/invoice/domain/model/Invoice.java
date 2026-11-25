package org.acme.invoice.domain.model;

import lombok.*;
import org.acme.domain.exceptions.ServiceException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Factura: dato estructurado de un documento de cobro (CU-002, CU-004). */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    private UUID id;
    private UUID userId;
    private UUID serviceId;
    private UUID documentId;
    private UUID extractionResultId;
    private String invoiceNumber;
    /** Período facturado, ej. 2026-12. */
    private String billingPeriod;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    @Builder.Default
    private String currency = "ARS";
    private String paymentCode;
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING_VALIDATION;
    @Builder.Default
    private InvoiceSource source = InvoiceSource.MANUAL;
    /** Referencia técnica de la IA, no garantía de exactitud. */
    private BigDecimal confidenceScore;
    private Instant confirmedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public void confirm(Instant now) {
        if (status == InvoiceStatus.REJECTED) {
            throw new ServiceException("No se puede confirmar una factura rechazada");
        }
        status = InvoiceStatus.CONFIRMED;
        confirmedAt = now;
    }

    public void reject() {
        if (status == InvoiceStatus.CONFIRMED) {
            throw new ServiceException("No se puede rechazar una factura confirmada");
        }
        status = InvoiceStatus.REJECTED;
    }
}
