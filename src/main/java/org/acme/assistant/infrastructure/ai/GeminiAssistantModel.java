package org.acme.assistant.infrastructure.ai;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.request.ChatRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.assistant.application.model.Answer;
import org.acme.assistant.application.model.AnswerListener;
import org.acme.assistant.application.port.output.AssistantModel;
import org.acme.assistant.domain.model.AssistantMessage;
import org.acme.assistant.domain.model.MessageRole;
import org.acme.domain.exceptions.UnavailableException;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * El asistente con Gemini: arma el historial, deja que el modelo llame herramientas y devuelve la respuesta.
 * Usa el modelo con streaming para mandar el texto a medida que llega.
 */
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

    /** Por ronda: Gemini ya corta a los 60 s (GEMINI_TIMEOUT); esto evita quedar colgado si no avisa. */
    static final long ROUND_SECONDS = 90;

    private final StreamingChatModel model;
    private final AssistantToolbox toolbox;
    private final Optional<String> apiKey;

    @Inject
    public GeminiAssistantModel(StreamingChatModel model, AssistantToolbox toolbox,
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
    public Answer answer(UUID userId, LocalDate today, ZoneId zone, List<AssistantMessage> history, AnswerListener listener) {
        AssistantTools tools = toolbox.forUser(userId, today);
        List<ToolSpecification> specs = ToolSpecifications.toolSpecificationsFrom(tools);
        String date = today.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-AR")));
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM.formatted(date, zone)));
        for (AssistantMessage m : history) {
            if (m.getRole() == MessageRole.USER) messages.add(UserMessage.from(m.getContent()));
            if (m.getRole() == MessageRole.ASSISTANT) messages.add(AiMessage.from(m.getContent()));
        }
        int tokens = 0;
        for (int round = 0; round < MAX_ROUNDS; round++) {
            ChatResponse response = chat(ChatRequest.builder().messages(messages).toolSpecifications(specs).build(), listener);
            if (response.tokenUsage() != null && response.tokenUsage().totalTokenCount() != null) {
                tokens += response.tokenUsage().totalTokenCount();
            }
            AiMessage reply = response.aiMessage();
            if (!reply.hasToolExecutionRequests()) {
                return new Answer(reply.text(), tools.references(), tools.used(), tokens);
            }
            messages.add(reply);
            for (ToolExecutionRequest request : reply.toolExecutionRequests()) {
                listener.tool(request.name());
                messages.add(ToolExecutionResultMessage.from(request, tools.execute(request)));
            }
        }
        throw new UnavailableException("No pude armar la respuesta, probá preguntarlo de otra forma");
    }

    /** Una ronda con streaming: manda el texto al listener y espera la respuesta completa. */
    private ChatResponse chat(ChatRequest request, AnswerListener listener) {
        CompletableFuture<ChatResponse> done = new CompletableFuture<>();
        try {
            model.chat(request, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partial) {
                    listener.text(partial);
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    done.complete(response);
                }

                @Override
                public void onError(Throwable error) {
                    done.completeExceptionally(error);
                }
            });
            return done.get(ROUND_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failed(e);
        } catch (ExecutionException e) {
            throw failed(e.getCause());
        } catch (TimeoutException | RuntimeException e) {
            throw failed(e);
        }
    }

    private static UnavailableException failed(Throwable e) {
        // Sin el contenido de la conversación en el log: solo el tipo de error
        LOG.errorf("Gemini no respondió al asistente: %s", e == null ? "?" : e.getClass().getSimpleName());
        return new UnavailableException("No pude responder ahora, probá de nuevo en un rato");
    }
}
