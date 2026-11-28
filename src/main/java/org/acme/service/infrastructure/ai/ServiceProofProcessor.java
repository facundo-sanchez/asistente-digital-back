package org.acme.service.infrastructure.ai;

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
import org.acme.service.application.model.ExtractedServiceProof;
import org.acme.shared.domain.model.DocumentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Set;
import java.util.UUID;

/** Lee el comprobante de un servicio con la IA y deja la propuesta para revisar en el alta del servicio. */
@ApplicationScoped
public class ServiceProofProcessor implements DocumentProcessor {

    private final FileStorage storage;
    private final DocumentReader reader;
    private final ExtractionResultRepository results;
    private final ObjectMapper json;
    private final String model;

    @Inject
    public ServiceProofProcessor(FileStorage storage, DocumentReader reader, ExtractionResultRepository results, ObjectMapper json,
                                 @ConfigProperty(name = "quarkus.langchain4j.ai.gemini.chat-model.model-id") String model) {
        this.storage = storage;
        this.reader = reader;
        this.results = results;
        this.json = json;
        this.model = model;
    }

    @Override
    public Set<DocumentType> supports() {
        return Set.of(DocumentType.SERVICE_PROOF);
    }

    @Override
    @Transactional
    public void process(Document document, UUID jobId) {
        String answer = reader.readJson(AiInputs.read(storage, document), ServiceProofPrompt.TEXT);
        results.save(AiExtractionResult.builder()
                .documentId(document.getId())
                .jobId(jobId)
                .userId(document.getUserId())
                .model(model)
                .schemaVersion(ExtractedServiceProof.SCHEMA_VERSION)
                .detectedType(DocumentType.SERVICE_PROOF)
                .extractedData(AiInputs.toMap(json, answer, ExtractedServiceProof.class))
                .build());
    }
}
