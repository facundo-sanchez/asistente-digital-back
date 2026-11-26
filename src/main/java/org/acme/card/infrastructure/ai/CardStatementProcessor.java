package org.acme.card.infrastructure.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.card.application.model.ExtractedStatement;
import org.acme.document.application.DocumentReadException;
import org.acme.document.application.FileType;
import org.acme.document.application.model.DocumentInput;
import org.acme.document.application.model.ReadFailure;
import org.acme.document.application.port.output.DocumentProcessor;
import org.acme.document.application.port.output.DocumentReader;
import org.acme.document.application.port.output.ExtractionResultRepository;
import org.acme.document.application.port.output.FileStorage;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.Document;
import org.acme.document.infrastructure.ai.SpreadsheetText;
import org.acme.shared.domain.model.DocumentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lee un resumen de tarjeta (PDF, imagen o Excel de cualquier banco) con la IA y deja la
 * propuesta para revisar. No crea nada confirmado (CU-004, RN-013).
 */
@ApplicationScoped
public class CardStatementProcessor implements DocumentProcessor {

    private final FileStorage storage;
    private final DocumentReader reader;
    private final ExtractionResultRepository results;
    private final ObjectMapper json;
    private final String model;

    @Inject
    public CardStatementProcessor(FileStorage storage, DocumentReader reader, ExtractionResultRepository results, ObjectMapper json,
                                  @ConfigProperty(name = "quarkus.langchain4j.ai.gemini.chat-model.model-id") String model) {
        this.storage = storage;
        this.reader = reader;
        this.results = results;
        this.json = json;
        this.model = model;
    }

    @Override
    public Set<DocumentType> supports() {
        return Set.of(DocumentType.CARD_STATEMENT);
    }

    @Override
    @Transactional
    public void process(Document document, UUID jobId) {
        String answer = reader.readJson(input(document), CardStatementPrompt.TEXT);
        Map<String, Object> data = parse(answer);
        results.save(AiExtractionResult.builder()
                .documentId(document.getId())
                .jobId(jobId)
                .userId(document.getUserId())
                .model(model)
                .schemaVersion(ExtractedStatement.SCHEMA_VERSION)
                .detectedType(DocumentType.CARD_STATEMENT)
                .extractedData(data)
                .build());
    }

    /** Excel → tabla de texto (cualquier formato de banco); PDF o imagen → el archivo tal cual. */
    private DocumentInput input(Document document) {
        try (InputStream in = storage.open(document.getStorageKey())) {
            boolean spreadsheet = FileType.XLSX.equals(document.getMimeType()) || FileType.XLS.equals(document.getMimeType());
            return spreadsheet
                    ? DocumentInput.ofText(SpreadsheetText.toText(in))
                    : DocumentInput.ofFile(document.getMimeType(), in.readAllBytes());
        } catch (IOException | RuntimeException e) {
            throw new DocumentReadException(ReadFailure.UNREADABLE_FILE, "No se pudo abrir el archivo: " + e.getClass().getSimpleName(), e);
        }
    }

    /** El JSON de la IA como mapa, comprobando que tenga la forma del resumen. Si vino en una lista, el primero. */
    Map<String, Object> parse(String answer) {
        try {
            JsonNode node = json.readTree(answer);
            if (node.isArray() && !node.isEmpty()) {
                node = node.get(0);
            }
            json.treeToValue(node, ExtractedStatement.class);
            return json.convertValue(node, new TypeReference<>() {
            });
        } catch (IOException | IllegalArgumentException e) {
            throw new DocumentReadException(ReadFailure.INVALID_RESPONSE, "La respuesta no tiene la forma de un resumen");
        }
    }
}
