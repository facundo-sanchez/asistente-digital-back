package org.acme.document.domain.model;

import lombok.*;
import org.acme.shared.domain.model.DocumentType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Inferencia de la IA sobre un documento. Se mantiene separada de la factura
 * confirmada para no confundir un dato inferido con uno validado (RN-006, RN-013).
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AiExtractionResult {
    private UUID id;
    private UUID documentId;
    private UUID jobId;
    private UUID userId;
    private String model;
    private String schemaVersion;
    private DocumentType detectedType;
    /** Datos extraídos según schemaVersion (ej. provider, invoiceNumber, dueDate, amount...). */
    @Builder.Default
    private Map<String, Object> extractedData = new HashMap<>();
    /** Confianza por campo, entre 0 y 1 (ej. {"amount": 0.99}). */
    @Builder.Default
    private Map<String, Double> confidenceData = new HashMap<>();
    @Builder.Default
    private ExtractionValidationStatus validationStatus = ExtractionValidationStatus.PENDING;
    private Instant createdAt;
    private Instant reviewedAt;
}
