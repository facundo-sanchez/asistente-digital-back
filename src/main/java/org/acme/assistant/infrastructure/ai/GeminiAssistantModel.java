package org.acme.assistant.infrastructure.ai;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.assistant.application.model.Answer;
import org.acme.assistant.application.port.output.AssistantModel;
import org.acme.assistant.domain.model.AssistantMessage;
import org.acme.assistant.domain.model.MessageRole;
import org.acme.domain.exceptions.ServiceException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** El asistente con Gemini: arma el historial, deja que el modelo llame herramientas y devuelve la respuesta. */
@ApplicationScoped
public class GeminiAssistantModel implements AssistantModel {

    private static final Logger LOG = Logger.getLogger(GeminiAssistantModel.class);
    /** Rondas de herramientas por respuesta: corta un modelo que se queda llamando. */
    static final int MAX_ROUNDS = 5;
    static final String SYSTEM = """
            Sos el asistente de "Asistente de Vida Digital", una app donde el usuario organiza servicios, facturas,
            vencimientos, tarjetas de crédito y documentos. Respondé en castellano rioplatense, corto y claro.
            Respondé solo con los datos que devuelven las herramientas: si no hay datos, decilo; nunca inventes
            importes, fechas ni nombres. Si la pregunta no es sobre sus datos de la app, explicá en una frase qué podés responder.
            Los importes en pesos y en dólares van por separado, nunca los sumes. Formato de montos: $ 48.500,00 o US$ 120,00.
            Fechas como "jueves 4/12". Usá listas cortas cuando haya varios registros. No pongas links: la app los muestra sola.
            Hoy es %s (zona horaria %s).""";

    private final ChatModel model;
    private final AssistantToolbox toolbox;
    private final Optional<String> apiKey;

    @Inject
    public GeminiAssistantModel(ChatModel model, AssistantToolbox toolbox,
                                @ConfigProperty(name = "quarkus.langchain4j.ai.gemini.api-key") Optional<String> apiKey) {
        this.model = model;
        this.toolbox = toolbox;
        this.apiKey = apiKey;
    }

    @Override
    public boolean available() {
        return apiKey.filter(key -> !key.isBlank()).isPresent();
    }

    @Override
    public Answer answer(UUID userId, LocalDate today, ZoneId zone, List<AssistantMessage> history) {
        AssistantTools tools = toolbox.forUser(userId, today);
        List<ToolSpecification> specs = ToolSpecifications.toolSpecificationsFrom(tools);
        String date = today.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-AR")));
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM.formatted(date, zone)));
        for (AssistantMessage m : history) {
            if (m.getRole() == MessageRole.USER) messages.add(UserMessage.from(m.getContent()));
            if (m.getRole() == MessageRole.ASSISTANT) messages.add(AiMessage.from(m.getContent()));
        }
        try {
            for (int round = 0; round < MAX_ROUNDS; round++) {
                AiMessage reply = model.chat(ChatRequest.builder().messages(messages).toolSpecifications(specs).build()).aiMessage();
                if (!reply.hasToolExecutionRequests()) {
                    return new Answer(reply.text(), tools.references(), tools.used());
                }
                messages.add(reply);
                for (ToolExecutionRequest request : reply.toolExecutionRequests()) {
                    messages.add(ToolExecutionResultMessage.from(request, tools.execute(request)));
                }
            }
        } catch (RuntimeException e) {
            // Sin el contenido de la conversación en el log: solo el tipo de error
            LOG.errorf("Gemini no respondió al asistente: %s", e.getClass().getSimpleName());
            throw new ServiceException("No pude responder ahora, probá de nuevo en un rato");
        }
        throw new ServiceException("No pude armar la respuesta, probá preguntarlo de otra forma");
    }
}
