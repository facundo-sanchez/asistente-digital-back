package org.acme.loan.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.document.application.port.output.DocumentProcessor;
import org.acme.document.application.port.output.DocumentReader;
import org.acme.document.application.port.output.ExtractionResultRepository;
import org.acme.document.application.port.output.FileStorage;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.Document;
import org.acme.document.infrastructure.ai.AiInputs;
import org.acme.loan.application.model.ExtractedLoanContract;
import org.acme.shared.domain.model.DocumentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Set;
import java.util.UUID;

/** Lee un comprobante de alta de préstamo con la IA y deja la propuesta para revisar (módulo 13). */
@ApplicationScoped
public class LoanContractProcessor implements DocumentProcessor {

    private final FileStorage storage;
    private final DocumentReader reader;
    private final ExtractionResultRepository results;
    private final ObjectMapper json;
    private final String model;

    @Inject
    public LoanContractProcessor(FileStorage storage, DocumentReader reader, ExtractionResultRepository results, ObjectMapper json,
                  @ConfigProperty(name = "quarkus.langchain4j.ai.gemini.chat-model.model-id") String model) {
        this.storage = storage;
        this.reader = reader;
        this.results = results;
        this.json = json;
        this.model = model;
    }

    @Override
    public Set<DocumentType> supports() {
        return Set.of(DocumentType.LOAN_CONTRACT);
    }

    @Override
    @Transactional
    public void process(Document document, UUID jobId) {
        String answer = reader.readJson(AiInputs.read(storage, document), LoanContractPrompt.TEXT);
        results.save(AiExtractionResult.builder()
                .documentId(document.getId())
                .jobId(jobId)
                .userId(document.getUserId())
                .model(model)
                .schemaVersion(ExtractedLoanContract.SCHEMA_VERSION)
                .detectedType(DocumentType.LOAN_CONTRACT)
                .extractedData(AiInputs.toMap(json, answer, ExtractedLoanContract.class))
                .build());
    }
}
