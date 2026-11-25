package org.acme.payment.infrastructure.rest.dto;

import org.acme.payment.domain.model.PaymentMethod;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "PaymentResponse")
public record PaymentResponse(
        UUID id,
        UUID obligationId,
        @Schema(example = "20000.00") BigDecimal amount,
        @Schema(example = "ARS") String currency,
        LocalDate paymentDate,
        PaymentMethod paymentMethod,
        UUID receiptDocumentId,
        String notes,
        boolean voided,
        Instant voidedAt,
        String voidReason,
        UUID replacesPaymentId,
        Instant createdAt,
        Instant updatedAt
) {}
