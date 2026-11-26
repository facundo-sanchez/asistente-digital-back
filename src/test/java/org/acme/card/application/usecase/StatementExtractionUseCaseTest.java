package org.acme.card.application.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.acme.card.application.BbvaSamples;
import org.acme.card.application.model.CardStatementView;
import org.acme.card.application.model.ExtractedStatement;
import org.acme.card.application.model.StatementConfirmation;
import org.acme.card.application.model.StatementReview;
import org.acme.card.domain.model.Card;
import org.acme.card.domain.model.CardBrand;
import org.acme.card.domain.model.CardStatementSource;
import org.acme.card.domain.model.CardTransaction;
import org.acme.card.domain.model.CardTransactionType;
import org.acme.document.application.usecase.InMemoryDocumentRepository;
import org.acme.document.application.usecase.InMemoryExtractionResultRepository;
import org.acme.document.application.usecase.InMemoryProcessingJobRepository;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.Document;
import org.acme.document.domain.model.DocumentProcessingJob;
import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.document.domain.model.ProcessingJobStatus;
import org.acme.document.domain.model.ProcessingStatus;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.shared.domain.model.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Revisar y confirmar el resumen de BBVA leído por la IA. */
class StatementExtractionUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();

    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private InMemoryExtractionResultRepository extractions;
    private InMemoryCardTransactionRepository transactions;
    private InMemoryDocumentRepository documents;
    private InMemoryProcessingJobRepository jobs;
    private StatementExtractionUseCase useCase;
    private UUID mastercard;
    private UUID galicia;

    @BeforeEach
    void setUp() {
        extractions = new InMemoryExtractionResultRepository();
        transactions = new InMemoryCardTransactionRepository();
        documents = new InMemoryDocumentRepository();
        jobs = new InMemoryProcessingJobRepository();
        InMemoryCardRepository cards = new InMemoryCardRepository();
        InMemoryCardStatementRepository statements = new InMemoryCardStatementRepository();
        galicia = UUID.randomUUID();
        mastercard = cards.save(Card.builder().userId(ANA).serviceId(galicia).name("Mastercard Black").brand(CardBrand.MASTERCARD)
                .accountLastFour("4521").build()).getId();
        Clock clock = Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC);
        var manage = new ManageCardStatementUseCase(statements, cards,
                new CardStatementDraftValidator(cards, statements, documents), new StatementObligations(new InMemoryObligationRepository()), clock);
        useCase = new StatementExtractionUseCase(extractions, cards, statements, transactions, manage, documents, jobs, json, clock);
    }

    @Test
    void sugiereLaTarjetaPorLaCuentaYNoTieneAvisos() {
        StatementReview review = useCase.review(ANA, extraction(BbvaSamples.pdf()));

        assertEquals(mastercard, review.suggestedCardId());
        assertEquals(List.of(), review.warnings());
        assertEquals(new BigDecimal("-53722.70"), review.transactionsTotal(), "pago + consumos + sellos");
    }

    @Test
    void confirmarCreaElResumenConSusMovimientosYCuotasYCierraElTrabajo() {
        UUID id = extraction(BbvaSamples.pdf());

        CardStatementView view = useCase.confirm(ANA, id, confirmation(BbvaSamples.pdf().dueDate()));

        assertEquals(CardStatementSource.AI_EXTRACTION, view.statement().getSource());
        assertEquals(new BigDecimal("216050.90"), view.statement().getTotalAmount());
        assertNotNull(view.statement().getObligationId());
        List<CardTransaction> saved = transactions.rows;
        assertEquals(8, saved.size());
        CardTransaction bringeri = saved.stream().filter(t -> "BRINGERI".equals(t.getMerchantName())).findFirst().orElseThrow();
        assertEquals(10, bringeri.getInstallmentNumber());
        assertEquals(12, bringeri.getInstallmentTotal());
        assertTrue(saved.stream().anyMatch(t -> t.getTransactionType() == CardTransactionType.PAYMENT && t.isCredit()));

        AiExtractionResult result = extractions.rows.get(id);
        assertEquals(ExtractionValidationStatus.CONFIRMED, result.getValidationStatus());
        assertEquals(ProcessingJobStatus.COMPLETED, jobs.rows.get(result.getJobId()).getStatus());
        Document document = documents.findByIdForUser(ANA, result.getDocumentId()).orElseThrow();
        assertEquals(ProcessingStatus.COMPLETED, document.getProcessingStatus());
        assertEquals(galicia, document.getServiceId());
        assertThrows(ServiceException.class, () -> useCase.confirm(ANA, id, confirmation(BbvaSamples.pdf().dueDate())), "ya revisada");
    }

    @Test
    void laPlanillaPideElVencimientoYCompletarloQuedaComoCorreccion() {
        UUID id = extraction(BbvaSamples.excel());

        assertThrows(ServiceException.class, () -> useCase.confirm(ANA, id, confirmation(null)));
        useCase.confirm(ANA, id, confirmation(LocalDate.of(2026, 11, 11)));

        assertEquals(ExtractionValidationStatus.CORRECTED, extractions.rows.get(id).getValidationStatus());
        assertEquals(7, transactions.rows.size());
    }

    @Test
    void descartarNoCreaNadaYOtroUsuarioNoLaVe() {
        UUID id = extraction(BbvaSamples.pdf());

        assertThrows(NotFoundException.class, () -> useCase.review(BETO, id));
        useCase.reject(ANA, id);

        assertEquals(ExtractionValidationStatus.REJECTED, extractions.rows.get(id).getValidationStatus());
        assertTrue(transactions.rows.isEmpty());
    }

    private StatementConfirmation confirmation(LocalDate dueDate) {
        ExtractedStatement pdf = BbvaSamples.pdf();
        return new StatementConfirmation(mastercard, pdf.closingDate(), dueDate, pdf.nextClosingDate(), pdf.nextDueDate(),
                pdf.totalAmount(), BigDecimal.ZERO, pdf.minimumPayment());
    }

    /** Una propuesta pendiente de un documento leído por un trabajo. */
    private UUID extraction(ExtractedStatement statement) {
        UUID document = documents.save(Document.builder().userId(ANA).documentType(DocumentType.CARD_STATEMENT)
                .originalFilename("resumen.pdf").checksum(UUID.randomUUID().toString()).storageKey("k")
                .processingStatus(ProcessingStatus.REQUIRES_REVIEW).build()).getId();
        UUID job = jobs.save(DocumentProcessingJob.builder().documentId(document).userId(ANA).status(ProcessingJobStatus.REQUIRES_REVIEW).build()).getId();
        Map<String, Object> data = json.convertValue(statement, new TypeReference<>() {
        });
        return extractions.save(AiExtractionResult.builder().documentId(document).jobId(job).userId(ANA).model("test")
                .schemaVersion(ExtractedStatement.SCHEMA_VERSION).detectedType(DocumentType.CARD_STATEMENT).extractedData(data).build()).getId();
    }
}
