package org.acme.invoice.infrastructure.rest.dto;

import org.acme.invoice.domain.model.InvoiceSource;
import org.acme.invoice.domain.model.InvoiceStatus;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "InvoiceResponse")
public record InvoiceResponse(
        UUID id,
        UUID serviceId,
        @Schema(description = "Nombre del servicio", example = "Luz casa") String serviceName,
        UUID documentId,
        UUID extractionResultId,
        @Schema(example = "0001-00001234") String invoiceNumber,
        @Schema(example = "2026-12") String billingPeriod,
        LocalDate issueDate,
        LocalDate dueDate,
        @Schema(example = "48500.00") BigDecimal totalAmount,
        @Schema(example = "ARS") String currency,
        String paymentCode,
        InvoiceStatus status,
        InvoiceSource source,
        @Schema(example = "0.97") BigDecimal confidenceScore,
        Instant confirmedAt,
        @Schema(description = "Vencimiento que generó la factura (null si no generó)") UUID obligationId,
        @Schema(description = "Estado del vencimiento: pendiente, pagado, vencido…") EffectiveObligationStatus obligationStatus,
        Instant createdAt,
        Instant updatedAt
) {}
