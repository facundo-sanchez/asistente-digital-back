package org.acme.document.infrastructure.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.acme.document.application.DocumentReadException;
import org.acme.document.application.FileType;
import org.acme.document.application.model.DocumentInput;
import org.acme.document.application.model.ReadFailure;
import org.acme.document.application.port.output.FileStorage;
import org.acme.document.domain.model.Document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Lo común a todos los procesadores: preparar el archivo para la IA y validar su respuesta. */
public final class AiInputs {

    /** Los emails importados de Gmail que traen la factura en el cuerpo. */
    public static final String TEXT = "text/plain";

    private AiInputs() {
    }

    /** Excel → tabla de texto (cualquier formato de banco o empresa); texto (un email) → tal cual; PDF o imagen → el archivo. */
    public static DocumentInput read(FileStorage storage, Document document) {
        try (InputStream in = storage.open(document.getStorageKey())) {
            boolean spreadsheet = FileType.XLSX.equals(document.getMimeType()) || FileType.XLS.equals(document.getMimeType());
            if (spreadsheet) {
                return DocumentInput.ofText(SpreadsheetText.toText(in));
            }
            return TEXT.equals(document.getMimeType())
                    ? DocumentInput.ofText(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    : DocumentInput.ofFile(document.getMimeType(), in.readAllBytes());
        } catch (IOException | RuntimeException e) {
            throw new DocumentReadException(ReadFailure.UNREADABLE_FILE, "No se pudo abrir el archivo: " + e.getClass().getSimpleName(), e);
        }
    }

    /**
     * El JSON de la IA como mapa, comprobando que tenga la forma de shape. Si vino en una lista,
     * se toma el primero.
     */
    public static Map<String, Object> toMap(ObjectMapper json, String answer, Class<?> shape) {
        try {
            JsonNode node = json.readTree(answer);
            if (node.isArray() && !node.isEmpty()) {
                node = node.get(0);
            }
            json.treeToValue(node, shape);
            return json.convertValue(node, new TypeReference<>() {
            });
        } catch (IOException | IllegalArgumentException e) {
            throw new DocumentReadException(ReadFailure.INVALID_RESPONSE, "La respuesta no tiene la forma esperada");
        }
    }
}
