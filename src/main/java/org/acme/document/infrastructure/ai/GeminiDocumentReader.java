package org.acme.document.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.acme.document.application.DocumentReadException;
import org.acme.document.application.model.DocumentInput;
import org.acme.document.application.model.ReadFailure;
import org.acme.document.application.port.output.DocumentReader;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/** {@link DocumentReader} con Gemini: PDF e imágenes van como archivo; las planillas, como texto. */
@ApplicationScoped
public class GeminiDocumentReader implements DocumentReader {

    static final String SYSTEM = """
            Sos un asistente que extrae datos de documentos financieros argentinos (resúmenes de tarjeta, facturas).
            Respondé SOLO con un objeto JSON válido, sin texto antes ni después, siguiendo exactamente las instrucciones.
            No inventes datos: si un dato no figura en el documento, usá null.
            Importes como número con punto decimal (ej. 216050.90), sin separador de miles. Fechas como yyyy-MM-dd.""";

    private final Instance<ChatModel> model;
    private final Optional<String> apiKey;
    private final ObjectMapper json = new ObjectMapper();

    @Inject
    public GeminiDocumentReader(Instance<ChatModel> model,
                                @ConfigProperty(name = "quarkus.langchain4j.ai.gemini.api-key") Optional<String> apiKey) {
        this.model = model;
        this.apiKey = apiKey;
    }

    @Override
    public boolean available() {
        return apiKey.filter(key -> !key.isBlank()).isPresent();
    }

    @Override
    public String readJson(DocumentInput input, String instructions) {
        if (!available()) {
            throw new DocumentReadException(ReadFailure.NOT_CONFIGURED, "No hay clave de Gemini configurada");
        }
        ChatRequest request = ChatRequest.builder()
                .messages(SystemMessage.from(SYSTEM), UserMessage.from(contents(input, instructions)))
                .responseFormat(ResponseFormat.JSON)
                .build();
        String answer;
        try {
            answer = model.get().chat(request).aiMessage().text();
        } catch (RuntimeException e) {
            // Sin el contenido del documento en el mensaje: solo el tipo de error
            throw new DocumentReadException(ReadFailure.UNAVAILABLE, "Gemini no respondió: " + e.getClass().getSimpleName(), e);
        }
        return validJson(answer);
    }

    private static List<Content> contents(DocumentInput input, String instructions) {
        TextContent ask = TextContent.from(instructions);
        if (input.isText()) {
            return List.of(ask, TextContent.from("Documento (planilla convertida a tabla de texto):\n" + input.text()));
        }
        String base64 = Base64.getEncoder().encodeToString(input.content());
        Content file = "application/pdf".equals(input.mimeType())
                ? PdfFileContent.from(base64, input.mimeType())
                : ImageContent.from(base64, input.mimeType());
        return List.of(ask, file);
    }

    /** Devuelve el JSON sin el envoltorio ```json que a veces agrega el modelo. */
    String validJson(String answer) {
        String text = answer == null ? "" : answer.strip();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        try {
            json.readTree(text);
            return text;
        } catch (IOException e) {
            throw new DocumentReadException(ReadFailure.INVALID_RESPONSE, "La respuesta de Gemini no es JSON válido");
        }
    }
}
