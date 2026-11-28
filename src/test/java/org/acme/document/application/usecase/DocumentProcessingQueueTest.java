package org.acme.document.application.usecase;

import org.acme.document.application.DocumentReadException;
import org.acme.document.application.model.ReadFailure;
import org.acme.document.domain.model.Document;
import org.acme.document.domain.model.DocumentProcessingJob;
import org.acme.document.domain.model.ProcessingJobStatus;
import org.acme.document.domain.model.ProcessingStatus;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.shared.domain.model.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Cola de lectura con IA: pedido, controles, procesamiento, reintentos y trabajos abandonados. */
class DocumentProcessingQueueTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-26T10:00:00Z"));
    private InMemoryDocumentRepository documents;
    private InMemoryProcessingJobRepository jobs;
    private FakeDocumentReader reader;
    private FakeDocumentProcessor processor;
    private DocumentProcessingUseCase processing;
    private ProcessingJobRunner runner;
    private UUID statement;

    @BeforeEach
    void setUp() {
        documents = new InMemoryDocumentRepository();
        jobs = new InMemoryProcessingJobRepository();
        reader = new FakeDocumentReader();
        processor = new FakeDocumentProcessor();
        processing = new DocumentProcessingUseCase(documents, jobs, reader, List.of(processor), new TestAiSettings(), clock);
        runner = new ProcessingJobRunner(jobs, documents, new TestAiSettings(), clock);
        statement = document(ANA, DocumentType.CARD_STATEMENT);
    }

    @Test
    void pedirLecturaEncolaUnaSolaVezYMarcaElDocumento() {
        DocumentProcessingJob job = processing.request(ANA, statement);

        assertEquals(job.getId(), processing.request(ANA, statement).getId(), "si ya hay una en curso, devuelve esa");
        assertEquals(1, jobs.rows.size());
        assertEquals(ProcessingStatus.PENDING, documents.findByIdForUser(ANA, statement).orElseThrow().getProcessingStatus());
        assertThrows(NotFoundException.class, () -> processing.request(BETO, statement));
    }

    @Test
    void controlesSinIaTipoNoSoportadoYTopeDiario() {
        assertThrows(ServiceException.class, () -> processing.request(ANA, document(ANA, DocumentType.CONTRACT)), "sin procesador");
        processing.request(ANA, document(ANA, DocumentType.CARD_STATEMENT));
        processing.request(ANA, statement);
        assertThrows(ServiceException.class, () -> processing.request(ANA, document(ANA, DocumentType.CARD_STATEMENT)), "tope de 2 por día");

        reader.available = false;
        assertFalse(processing.availability().available());
        assertThrows(ServiceException.class, () -> processing.checkCanProcess(BETO, DocumentType.CARD_STATEMENT));
    }

    @Test
    void sePuedeVolverALeerComoOtroTipoSinSubirloDeNuevo() {
        UUID proof = documents.save(Document.builder().userId(ANA).documentType(DocumentType.CONTRACT).originalFilename("luz.pdf")
                .checksum("x").storageKey("k").serviceId(UUID.randomUUID()).build()).getId();

        assertThrows(ServiceException.class, () -> processing.requestAs(ANA, proof, DocumentType.POLICY), "tipo sin procesador");
        assertEquals(DocumentType.CONTRACT, documents.findByIdForUser(ANA, proof).orElseThrow().getDocumentType(), "no cambia si no se puede leer");

        processing.requestAs(ANA, proof, DocumentType.CARD_STATEMENT);

        Document document = documents.findByIdForUser(ANA, proof).orElseThrow();
        assertEquals(DocumentType.CARD_STATEMENT, document.getDocumentType());
        assertNotNull(document.getServiceId(), "conserva el servicio");
        assertEquals(ProcessingStatus.PENDING, document.getProcessingStatus());
    }

    @Test
    void terminaListoParaRevisar() {
        UUID id = processing.request(ANA, statement).getId();

        process(runner.claimNext().orElseThrow());

        assertEquals(ProcessingJobStatus.REQUIRES_REVIEW, jobs.rows.get(id).getStatus());
        assertEquals(ProcessingStatus.REQUIRES_REVIEW, documents.findByIdForUser(ANA, statement).orElseThrow().getProcessingStatus());
        assertTrue(runner.claimNext().isEmpty());
    }

    @Test
    void unCorteSeReintentaMasTardeYUnErrorDefinitivoNo() {
        UUID id = processing.request(ANA, statement).getId();
        processor.failWith = new DocumentReadException(ReadFailure.UNAVAILABLE, "corte");

        process(runner.claimNext().orElseThrow());
        DocumentProcessingJob failed = jobs.rows.get(id);
        assertEquals(ProcessingJobStatus.FAILED, failed.getStatus());
        assertEquals(clock.instant().plusSeconds(60), failed.getNextRetryAt(), "primer reintento al minuto");
        assertTrue(runner.claimNext().isEmpty(), "todavía no es hora de reintentar");

        clock.advanceSeconds(61);
        processor.failWith = new DocumentReadException(ReadFailure.NOT_CONFIGURED, "sin clave");
        process(runner.claimNext().orElseThrow());
        assertEquals(ProcessingJobStatus.DEAD, jobs.rows.get(id).getStatus());
        assertEquals("NOT_CONFIGURED", jobs.rows.get(id).getErrorCode());
        assertEquals(ProcessingStatus.FAILED, documents.findByIdForUser(ANA, statement).orElseThrow().getProcessingStatus());
    }

    @Test
    void unTrabajoAbandonadoSeRetomaYEnSuUltimoIntentoQuedaMuerto() {
        UUID id = processing.request(ANA, statement).getId();
        runner.claimNext().orElseThrow(); // se toma y el proceso "se cae" sin terminar

        assertTrue(runner.claimNext().isEmpty(), "bloqueado mientras dura el lock");
        clock.advanceSeconds(11 * 60);
        assertEquals(2, runner.claimNext().orElseThrow().getAttempts(), "se retoma después del lock");

        DocumentProcessingJob job = jobs.rows.get(id);
        job.setAttempts(job.getMaxAttempts());
        clock.advanceSeconds(11 * 60);
        assertTrue(runner.claimNext().isEmpty());
        assertEquals(ProcessingJobStatus.DEAD, jobs.rows.get(id).getStatus());
    }

    /** Lo que hace la tarea programada con un trabajo tomado. */
    private void process(DocumentProcessingJob job) {
        try {
            processor.process(documents.findByIdForUser(job.getUserId(), job.getDocumentId()).orElseThrow(), job.getId());
            runner.succeeded(job.getId());
        } catch (DocumentReadException e) {
            runner.failed(job.getId(), e.failure().name(), e.getMessage(), e.retryable());
        }
    }

    private UUID document(UUID userId, DocumentType type) {
        return documents.save(Document.builder().userId(userId).documentType(type).originalFilename("r.pdf")
                .checksum(UUID.randomUUID().toString()).storageKey("k").build()).getId();
    }

}
