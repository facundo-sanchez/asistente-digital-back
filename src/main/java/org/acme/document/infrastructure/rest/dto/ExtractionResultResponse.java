package org.acme.document.infrastructure.rest.dto;

import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.shared.domain.model.DocumentType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Propuesta de la IA para que el usuario la revise antes de confirmar. */
@Schema(name = "ExtractionResultResponse")
public record ExtractionResultResponse(
        UUID id,
        UUID documentId,
        @Schema(example = "gemini-2.5-flash") String model,
        @Schema(example = "1") String schemaVersion,
        DocumentType detectedType,
        @Schema(description = "Datos extraídos (provider, invoiceNumber, dueDate, amount...)")
        Map<String, Object> extractedData,
        @Schema(description = "Confianza por campo, de 0 a 1", example = "{\"amount\": 0.99, \"dueDate\": 0.97}")
        Map<String, Double> confidenceData,
        ExtractionValidationStatus validationStatus,
        Instant createdAt,
        Instant reviewedAt
) {}
