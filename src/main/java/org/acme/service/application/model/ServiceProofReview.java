package org.acme.service.application.model;

import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.provider.domain.model.Provider;

import java.util.List;
import java.util.UUID;

/** Lo que leyó la IA del comprobante, el proveedor del catálogo que coincide (si hay) y avisos para revisar. */
public record ServiceProofReview(UUID extractionId, UUID documentId, ExtractionValidationStatus status, ExtractedServiceProof proof,
                                 Provider provider, List<String> warnings) {
}
