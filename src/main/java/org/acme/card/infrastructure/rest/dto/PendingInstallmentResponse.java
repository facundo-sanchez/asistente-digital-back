package org.acme.card.infrastructure.rest.dto;

import org.acme.card.domain.model.SpendingCategory;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "PendingInstallmentResponse")
public record PendingInstallmentResponse(
        @Schema(description = "Id del último movimiento registrado de la compra") UUID id,
        UUID cardId,
        UUID purchaseGroupId,
        LocalDate purchaseDate,
        @Schema(example = "TIENDA Y") String merchant,
        SpendingCategory category,
        @Schema(example = "ARS") String currency,
        @Schema(example = "18000.00") BigDecimal installmentAmount,
        @Schema(description = "Última cuota registrada", example = "3") Integer installmentNumber,
        @Schema(example = "6") Integer installmentTotal,
        @Schema(example = "3") Integer remainingInstallments,
        @Schema(example = "54000.00") BigDecimal remainingAmount,
        BigDecimal originalAmount,
        LocalDate lastClosingDate,
        @Schema(description = "Cierre estimado del resumen con la última cuota") LocalDate estimatedLastClosingDate
) {}
