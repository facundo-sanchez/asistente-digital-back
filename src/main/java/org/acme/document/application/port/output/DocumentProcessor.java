package org.acme.document.application.port.output;

import org.acme.document.domain.model.Document;
import org.acme.shared.domain.model.DocumentType;

import java.util.Set;
import java.util.UUID;

/**
 * Lee un tipo de documento con IA y deja la propuesta para revisar (ai_extraction_results).
 * Hay uno por tipo: resúmenes de tarjeta (9.3), facturas (9.5).
 * Si falla, lanza {@link org.acme.document.application.DocumentReadException}.
 */
public interface DocumentProcessor {

    Set<DocumentType> supports();

    void process(Document document, UUID jobId);
}
