package org.acme.service.infrastructure.rest.dto;

import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.provider.infrastructure.rest.dto.ProviderResponse;
import org.acme.service.application.model.ExtractedServiceProof;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/** GET /service-extractions/{id}: lo leído del comprobante, el proveedor que coincide (o null) y avisos. */
@Schema(name = "ServiceProofReviewResponse")
public record ServiceProofReviewResponse(UUID extractionId, UUID documentId, ExtractionValidationStatus status, ExtractedServiceProof proof,
                                         ProviderResponse provider, List<String> warnings) {
}
