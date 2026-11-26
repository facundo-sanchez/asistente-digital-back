package org.acme.document.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.application.port.output.ExtractionResultRepository;
import org.acme.document.application.port.output.ProcessingJobRepository;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.document.domain.model.ProcessingStatus;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Lo común a revisar cualquier propuesta de la IA (resumen o factura): buscarla y cerrarla. */
@ApplicationScoped
public class ExtractionReviews {

    static final String ALREADY_REVIEWED = "Esta propuesta ya fue revisada";
    static final String WRONG_TYPE = "La propuesta no es de este tipo de documento";

    private final ExtractionResultRepository extractions;
    private final DocumentRepository documents;
    private final ProcessingJobRepository jobs;
    private final Clock clock;

    @Inject
    public ExtractionReviews(ExtractionResultRepository extractions, DocumentRepository documents, ProcessingJobRepository jobs, Clock clock) {
        this.extractions = extractions;
        this.documents = documents;
        this.jobs = jobs;
        this.clock = clock;
    }

    /** La última propuesta de un documento (para ir a revisarla desde el documento). */
    public Optional<AiExtractionResult> latestForDocument(UUID userId, UUID documentId) {
        return extractions.findLatestForDocument(userId, documentId);
    }

    /** Una propuesta de otro usuario responde igual que una inexistente. */
    public AiExtractionResult find(UUID userId, UUID extractionId, String schemaVersion) {
        AiExtractionResult result = extractions.findByIdForUser(userId, extractionId).orElseThrow(() -> new NotFoundException("propuesta"));
        if (!schemaVersion.equals(result.getSchemaVersion())) {
            throw new ServiceException(WRONG_TYPE);
        }
        return result;
    }

    public AiExtractionResult pending(UUID userId, UUID extractionId, String schemaVersion) {
        AiExtractionResult result = find(userId, extractionId, schemaVersion);
        if (result.getValidationStatus() != ExtractionValidationStatus.PENDING) {
            throw new ServiceException(ALREADY_REVIEWED);
        }
        return result;
    }

    /**
     * Marca la propuesta como revisada (status), cierra su trabajo y deja el documento leído y
     * asociado al servicio (si todavía no tenía uno).
     */
    public void close(UUID userId, AiExtractionResult result, ExtractionValidationStatus status, UUID serviceId) {
        Instant now = Instant.now(clock);
        result.setValidationStatus(status);
        result.setReviewedAt(now);
        extractions.save(result);
        if (result.getJobId() != null) {
            jobs.findById(result.getJobId()).ifPresent(job -> {
                job.complete(now);
                jobs.save(job);
            });
        }
        documents.findByIdForUser(userId, result.getDocumentId()).ifPresent(document -> {
            document.setProcessingStatus(ProcessingStatus.COMPLETED);
            if (document.getServiceId() == null) {
                document.setServiceId(serviceId);
            }
            documents.save(document);
        });
    }
}
