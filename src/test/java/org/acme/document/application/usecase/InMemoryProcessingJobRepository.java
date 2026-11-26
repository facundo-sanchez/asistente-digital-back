package org.acme.document.application.usecase;

import org.acme.document.application.port.output.ProcessingJobRepository;
import org.acme.document.domain.model.DocumentProcessingJob;
import org.acme.document.domain.model.ProcessingJobStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Cola en memoria con el mismo criterio de "listo para procesar" que el adaptador real. */
public class InMemoryProcessingJobRepository implements ProcessingJobRepository {

    public final Map<UUID, DocumentProcessingJob> rows = new LinkedHashMap<>();
    /** "Hoy" de los trabajos creados: lo fija el test. */
    Instant createdAt = Instant.parse("2026-11-26T10:00:00Z");

    @Override
    public DocumentProcessingJob save(DocumentProcessingJob job) {
        DocumentProcessingJob copy = job.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
            copy.setCreatedAt(createdAt);
            createdAt = createdAt.plusSeconds(1);
        }
        rows.put(copy.getId(), copy);
        return copy.toBuilder().build();
    }

    @Override
    public Optional<DocumentProcessingJob> findLatestForDocument(UUID userId, UUID documentId) {
        return rows.values().stream()
                .filter(j -> j.getUserId().equals(userId) && j.getDocumentId().equals(documentId))
                .max(Comparator.comparing(DocumentProcessingJob::getCreatedAt))
                .map(j -> j.toBuilder().build());
    }

    @Override
    public Optional<DocumentProcessingJob> lockNextReady(Instant now) {
        return rows.values().stream()
                .filter(j -> j.getStatus() == ProcessingJobStatus.PENDING
                        || (j.getStatus() == ProcessingJobStatus.FAILED && !j.getNextRetryAt().isAfter(now))
                        || (j.getStatus() == ProcessingJobStatus.PROCESSING && j.getLockedUntil().isBefore(now)))
                .min(Comparator.comparing(DocumentProcessingJob::getCreatedAt))
                .map(j -> j.toBuilder().build());
    }

    @Override
    public Optional<DocumentProcessingJob> findById(UUID jobId) {
        return Optional.ofNullable(rows.get(jobId)).map(j -> j.toBuilder().build());
    }

    @Override
    public long countCreatedSince(UUID userId, Instant since) {
        return rows.values().stream().filter(j -> j.getUserId().equals(userId) && !j.getCreatedAt().isBefore(since)).count();
    }
}
