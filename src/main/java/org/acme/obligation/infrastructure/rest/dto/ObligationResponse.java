package org.acme.obligation.infrastructure.rest.dto;

import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.obligation.domain.model.ObligationType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "ObligationResponse")
public record ObligationResponse(
        UUID id,
        UUID serviceId,
        @Schema(description = "Nombre del servicio asociado (null si no tiene)", example = "Luz casa") String serviceName,
        UUID invoiceId,
        UUID assetId,
        UUID recurrenceId,
        @Schema(example = "Luz diciembre") String title,
        String description,
        ObligationType obligationType,
        @Schema(example = "48500.00") BigDecimal amount,
        @Schema(example = "ARS") String currency,
        LocalDate dueDate,
        @Schema(description = "Estado persistido") ObligationStatus status,
        @Schema(description = "Estado a mostrar (incluye DUE_SOON y OVERDUE)") EffectiveObligationStatus effectiveStatus,
        @Schema(description = "Total pagado (pagos no anulados)", example = "20000.00") BigDecimal paidAmount,
        @Schema(description = "Saldo pendiente; null si no tiene importe", example = "28500.00") BigDecimal balance,
        @Schema(description = "Anticipada por recurrencia, sin factura real") boolean estimated,
        Instant cancelledAt,
        String cancelReason,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt
) {}
