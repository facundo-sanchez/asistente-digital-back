package org.acme.document.application.port.output;

import org.acme.document.domain.model.AiExtractionResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Propuestas de la IA (ai_extraction_results), separadas del dato confirmado (RN-006, RN-013). */
public interface ExtractionResultRepository {

    /** Inserta si no tiene id; si no, actualiza. */
    AiExtractionResult save(AiExtractionResult result);

    Optional<AiExtractionResult> findByIdForUser(UUID userId, UUID resultId);

    /** La última propuesta del documento. */
    Optional<AiExtractionResult> findLatestForDocument(UUID userId, UUID documentId);

    /** Las del usuario que esperan revisión, las más viejas primero. */
    List<AiExtractionResult> findPending(UUID userId);
}
