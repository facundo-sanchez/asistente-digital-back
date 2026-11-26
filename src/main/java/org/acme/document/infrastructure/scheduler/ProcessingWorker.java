package org.acme.document.infrastructure.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.document.application.DocumentReadException;
import org.acme.document.application.port.output.DocumentProcessor;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.application.usecase.DocumentProcessingUseCase;
import org.acme.document.application.usecase.ProcessingJobRunner;
import org.acme.document.domain.model.Document;
import org.acme.document.domain.model.DocumentProcessingJob;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Procesa la cola de lecturas con IA: toma un trabajo, lo lee con el procesador de su tipo y
 * anota cómo terminó. Los fallos se reintentan solos (ver DocumentProcessingJob.fail).
 */
@ApplicationScoped
public class ProcessingWorker {

    private static final Logger LOG = Logger.getLogger(ProcessingWorker.class);
    /** Trabajos por corrida: acota cuánto tarda cada una. */
    static final int BATCH = 5;

    @Inject
    ProcessingJobRunner runner;

    @Inject
    DocumentProcessingUseCase processing;

    @Inject
    DocumentRepository documents;

    @Scheduled(every = "{app.ai.poll-every:20s}", delayed = "20s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        for (int i = 0; i < BATCH; i++) {
            Optional<DocumentProcessingJob> next = runner.claimNext();
            if (next.isEmpty()) {
                return;
            }
            process(next.get());
        }
    }

    void process(DocumentProcessingJob job) {
        try {
            Document document = documents.findByIdForUser(job.getUserId(), job.getDocumentId())
                    .orElseThrow(() -> new IllegalStateException("El documento ya no existe"));
            DocumentProcessor processor = processing.processorFor(document.getDocumentType())
                    .orElseThrow(() -> new IllegalStateException("Sin procesador para " + document.getDocumentType()));
            processor.process(document, job.getId());
            runner.succeeded(job.getId());
        } catch (DocumentReadException e) {
            runner.failed(job.getId(), e.failure().name(), e.getMessage(), e.retryable());
        } catch (IllegalStateException e) {
            runner.failed(job.getId(), "NOT_PROCESSABLE", e.getMessage(), false);
        } catch (RuntimeException e) {
            // Sin el contenido del documento: solo el tipo de error
            LOG.errorf("Falló la lectura del documento %s: %s", job.getDocumentId(), e.getClass().getSimpleName());
            runner.failed(job.getId(), "INTERNAL", e.getClass().getSimpleName(), true);
        }
    }
}
