package org.acme.document.infrastructure.persistence.repository;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import org.acme.document.application.port.output.ProcessingJobRepository;
import org.acme.document.domain.model.DocumentProcessingJob;
import org.acme.document.domain.model.ProcessingJobStatus;
import org.acme.document.infrastructure.persistence.entity.DocumentProcessingJobEntity;
import org.acme.document.infrastructure.persistence.mapper.DocumentPersistenceMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de persistencia del puerto {@link ProcessingJobRepository}. */
@ApplicationScoped
public class ProcessingJobRepositoryAdapter implements ProcessingJobRepository {

    /** Hint de Hibernate: si otra transacción ya bloqueó la fila, saltearla (SKIP LOCKED). */
    private static final String LOCK_TIMEOUT = "jakarta.persistence.lock.timeout";
    private static final int SKIP_LOCKED = -2;

    @Inject
    ProcessingJobPanacheRepository repository;

    @Inject
    DocumentPersistenceMapper mapper;

    @Override
    public DocumentProcessingJob save(DocumentProcessingJob job) {
        DocumentProcessingJobEntity entity = mapper.toEntity(job);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    public Optional<DocumentProcessingJob> findLatestForDocument(UUID userId, UUID documentId) {
        return repository.find("userId = ?1 and documentId = ?2", Sort.descending("createdAt"), userId, documentId)
                .firstResultOptional()
                .map(mapper::toModel);
    }

    @Override
    public Optional<DocumentProcessingJob> lockNextReady(Instant now) {
        return repository.find("status = :pending"
                                + " or (status = :failed and nextRetryAt <= :now)"
                                + " or (status = :processing and lockedUntil < :now)",
                        Sort.by("createdAt"),
                        Parameters.with("pending", ProcessingJobStatus.PENDING)
                                .and("failed", ProcessingJobStatus.FAILED)
                                .and("processing", ProcessingJobStatus.PROCESSING)
                                .and("now", now))
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .withHint(LOCK_TIMEOUT, SKIP_LOCKED)
                .firstResultOptional()
                .map(mapper::toModel);
    }

    @Override
    public Optional<DocumentProcessingJob> findById(UUID jobId) {
        return repository.findByIdOptional(jobId).map(mapper::toModel);
    }

    @Override
    public long countCreatedSince(UUID userId, Instant since) {
        return repository.count("userId = ?1 and createdAt >= ?2", userId, since);
    }
}
