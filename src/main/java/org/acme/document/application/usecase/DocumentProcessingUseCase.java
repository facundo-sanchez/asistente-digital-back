package org.acme.document.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.document.application.AiSettings;
import org.acme.document.application.model.AiAvailability;
import org.acme.document.application.port.output.DocumentProcessor;
import org.acme.document.application.port.output.DocumentReader;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.application.port.output.ProcessingJobRepository;
import org.acme.document.domain.model.Document;
import org.acme.document.domain.model.DocumentProcessingJob;
import org.acme.document.domain.model.ProcessingStatus;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.shared.domain.model.DocumentType;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Pedir que la IA lea un documento (encola un trabajo) y consultar cómo va. La lectura la hace
 * después la tarea programada, con reintentos. El pedido de lectura es el consentimiento del
 * usuario para enviar ese archivo a la IA (RN-007).
 */
@ApplicationScoped
public class DocumentProcessingUseCase {

    static final String NOT_AVAILABLE = "La lectura con IA no está disponible en este momento";
    static final String UNSUPPORTED_TYPE = "La IA todavía no lee este tipo de documento";
    static final String DAILY_LIMIT = "Llegaste al máximo de %d lecturas con IA por hoy. Probá mañana o cargalo a mano";

    private final DocumentRepository documents;
    private final ProcessingJobRepository jobs;
    private final DocumentReader reader;
    private final List<DocumentProcessor> processors;
    private final AiSettings settings;
    private final Clock clock;

    @Inject
    public DocumentProcessingUseCase(DocumentRepository documents, ProcessingJobRepository jobs, DocumentReader reader,
                                     @Any Instance<DocumentProcessor> processors, AiSettings settings, Clock clock) {
        this(documents, jobs, reader, StreamSupport.stream(processors.spliterator(), false).toList(), settings, clock);
    }

    DocumentProcessingUseCase(DocumentRepository documents, ProcessingJobRepository jobs, DocumentReader reader,
                              List<DocumentProcessor> processors, AiSettings settings, Clock clock) {
        this.documents = documents;
        this.jobs = jobs;
        this.reader = reader;
        this.processors = processors;
        this.settings = settings;
        this.clock = clock;
    }

    /** Disponible si hay clave de IA; los tipos son los que tienen un procesador. */
    public AiAvailability availability() {
        Set<DocumentType> types = EnumSet.noneOf(DocumentType.class);
        processors.forEach(p -> types.addAll(p.supports()));
        return new AiAvailability(reader.available() && !types.isEmpty(), types);
    }

    /** Lanza si no se puede leer ese tipo ahora (sin IA, tipo no soportado o tope diario). Se usa antes de subir. */
    public void checkCanProcess(UUID userId, DocumentType type) {
        AiAvailability availability = availability();
        if (!availability.available()) {
            throw new ServiceException(NOT_AVAILABLE);
        }
        if (!availability.documentTypes().contains(type)) {
            throw new ServiceException(UNSUPPORTED_TYPE);
        }
        Instant today = Instant.now(clock).truncatedTo(ChronoUnit.DAYS);
        if (jobs.countCreatedSince(userId, today) >= settings.dailyLimit()) {
            throw new ServiceException(DAILY_LIMIT.formatted(settings.dailyLimit()));
        }
    }

    /** Encola la lectura. Si ya hay una en curso para el documento, devuelve esa. */
    @Transactional
    public DocumentProcessingJob request(UUID userId, UUID documentId) {
        Document document = documents.findByIdForUser(userId, documentId).orElseThrow(() -> new NotFoundException("documento"));
        Optional<DocumentProcessingJob> current = jobs.findLatestForDocument(userId, documentId).filter(DocumentProcessingJob::isActive);
        if (current.isPresent()) {
            return current.get();
        }
        checkCanProcess(userId, document.getDocumentType());
        DocumentProcessingJob job = jobs.save(DocumentProcessingJob.builder()
                .documentId(documentId)
                .userId(userId)
                .provider("gemini")
                .build());
        document.setProcessingStatus(ProcessingStatus.PENDING);
        documents.save(document);
        return job;
    }

    /**
     * Vuelve a leer el documento como otro tipo, sin subirlo de nuevo (ej. la factura con la que se creó un servicio,
     * ahora como factura). Conserva el servicio asociado. Si ya hay una lectura en curso, no cambia nada.
     */
    @Transactional
    public DocumentProcessingJob requestAs(UUID userId, UUID documentId, DocumentType type) {
        Document document = documents.findByIdForUser(userId, documentId).orElseThrow(() -> new NotFoundException("documento"));
        if (jobs.findLatestForDocument(userId, documentId).filter(DocumentProcessingJob::isActive).isEmpty() && document.getDocumentType() != type) {
            checkCanProcess(userId, type);
            document.setDocumentType(type);
            documents.save(document);
        }
        return request(userId, documentId);
    }

    /** El último trabajo del documento, si alguna vez se pidió leerlo. */
    public Optional<DocumentProcessingJob> status(UUID userId, UUID documentId) {
        documents.findByIdForUser(userId, documentId).orElseThrow(() -> new NotFoundException("documento"));
        return jobs.findLatestForDocument(userId, documentId);
    }

    /** El procesador del tipo de documento, si hay. */
    public Optional<DocumentProcessor> processorFor(DocumentType type) {
        return processors.stream().filter(p -> p.supports().contains(type)).findFirst();
    }
}
