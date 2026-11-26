package org.acme.document.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.document.application.AiSettings;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.application.port.output.ProcessingJobRepository;
import org.acme.document.domain.model.DocumentProcessingJob;
import org.acme.document.domain.model.ProcessingJobStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Los pasos de la cola que tocan la base, cada uno en su transacción corta: tomar el próximo
 * trabajo y anotar cómo terminó. La lectura con IA (lenta) pasa entre los dos, fuera de transacción.
 */
@ApplicationScoped
public class ProcessingJobRunner {

    static final String TOO_MANY_ATTEMPTS = "TOO_MANY_ATTEMPTS";

    private final ProcessingJobRepository jobs;
    private final DocumentRepository documents;
    private final AiSettings settings;
    private final Clock clock;

    @Inject
    public ProcessingJobRunner(ProcessingJobRepository jobs, DocumentRepository documents, AiSettings settings, Clock clock) {
        this.jobs = jobs;
        this.documents = documents;
        this.settings = settings;
        this.clock = clock;
    }

    /** Toma el próximo trabajo listo (lo bloquea hasta settings.lock) o vacío si no hay. */
    @Transactional
    public Optional<DocumentProcessingJob> claimNext() {
        Instant now = Instant.now(clock);
        Optional<DocumentProcessingJob> next = jobs.lockNextReady(now);
        next.ifPresent(job -> {
            if (job.getAttempts() >= job.getMaxAttempts()) {
                // Abandonado en su último intento: no se vuelve a tomar
                job.fail(now, TOO_MANY_ATTEMPTS, "Superó el máximo de intentos", false);
            } else {
                job.start(now, settings.lock());
            }
            save(job);
        });
        return next.filter(job -> job.getStatus() == ProcessingJobStatus.PROCESSING);
    }

    @Transactional
    public void succeeded(UUID jobId) {
        jobs.findById(jobId).ifPresent(job -> {
            job.succeed(Instant.now(clock));
            save(job);
        });
    }

    @Transactional
    public void failed(UUID jobId, String code, String message, boolean retryable) {
        jobs.findById(jobId).ifPresent(job -> {
            job.fail(Instant.now(clock), code, message, retryable);
            save(job);
        });
    }

    /** Guarda el trabajo y deja el estado del documento al día. */
    private void save(DocumentProcessingJob job) {
        jobs.save(job);
        documents.findByIdForUser(job.getUserId(), job.getDocumentId()).ifPresent(document -> {
            document.setProcessingStatus(job.documentStatus());
            documents.save(document);
        });
    }
}
