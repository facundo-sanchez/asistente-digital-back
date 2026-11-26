package org.acme.document.application.port.output;

import org.acme.document.domain.model.DocumentProcessingJob;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Cola de trabajos de lectura con IA (document_processing_jobs). */
public interface ProcessingJobRepository {

    /** Inserta si no tiene id; si no, actualiza. */
    DocumentProcessingJob save(DocumentProcessingJob job);

    /** El último trabajo del documento del usuario. */
    Optional<DocumentProcessingJob> findLatestForDocument(UUID userId, UUID documentId);

    /**
     * El próximo trabajo listo para procesar, bloqueado para esta transacción (otros procesos lo
     * saltean): pendiente, fallido con reintento vencido, o en proceso pero abandonado.
     */
    Optional<DocumentProcessingJob> lockNextReady(Instant now);

    Optional<DocumentProcessingJob> findById(UUID jobId);

    /** Cuántos trabajos pidió el usuario desde ese momento (tope diario). */
    long countCreatedSince(UUID userId, Instant since);
}
