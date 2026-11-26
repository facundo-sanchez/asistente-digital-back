package org.acme.card.application.model;

import org.acme.document.domain.model.ExtractionValidationStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Lo que ve el usuario para revisar un resumen leído por la IA: la propuesta, la tarjeta que le
 * corresponde (si se encontró por los últimos 4), los avisos de los controles y los totales.
 */
public record StatementReview(
        UUID extractionId,
        UUID documentId,
        ExtractionValidationStatus status,
        ExtractedStatement statement,
        UUID suggestedCardId,
        List<String> warnings,
        BigDecimal transactionsTotal,
        BigDecimal transactionsTotalUsd,
        UUID statementId
) {}
