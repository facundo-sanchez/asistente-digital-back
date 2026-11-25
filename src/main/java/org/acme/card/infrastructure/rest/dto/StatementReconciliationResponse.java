package org.acme.card.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

/** Un elemento de GET /card-statements/{id}/reconciliation (uno por moneda). */
@Schema(name = "StatementReconciliationResponse")
public record StatementReconciliationResponse(
        @Schema(example = "ARS") String currency,
        @Schema(description = "Total informado en el resumen", example = "315000.00") BigDecimal statementTotal,
        @Schema(description = "Suma de los consumos cargados", example = "314500.00") BigDecimal transactionsTotal,
        @Schema(description = "statementTotal - transactionsTotal", example = "500.00") BigDecimal difference,
        boolean balanced
) {}
