package org.acme.assistant.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.assistant.application.model.Answer;
import org.acme.assistant.application.model.AnswerListener;
import org.acme.assistant.application.model.ConversationDetail;
import org.acme.assistant.application.port.output.AssistantMessageRepository;
import org.acme.assistant.application.port.output.AssistantModel;
import org.acme.assistant.application.port.output.ConversationRepository;
import org.acme.assistant.domain.model.AssistantMessage;
import org.acme.assistant.domain.model.Conversation;
import org.acme.assistant.domain.model.MessageRole;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.TooManyRequestsException;
import org.acme.domain.exceptions.UnavailableException;
import org.acme.user.application.port.output.UserRepository;
import org.acme.user.domain.model.User;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Conversaciones con el asistente (CU-008). */
@ApplicationScoped
public class AssistantUseCase {

    /** Mensajes de la conversación que ve el modelo (límite de tokens y costo). */
    static final int HISTORY = 20;
    static final int TITLE_LENGTH = 60;
    /** Claves de metadata de la respuesta: registros usados (para los links) y herramientas llamadas. */
    public static final String METADATA_REFERENCES = "references";
    public static final String METADATA_TOOLS = "tools";
    public static final String METADATA_TOKENS = "tokens";
    /** Preguntas por usuario por hora: la clave de Gemini es una sola y tiene cuota. */
    static final int QUESTIONS_PER_HOUR = 30;
    static final String TOO_MANY = "Llegaste al límite de consultas por hora, probá en un rato";
    static final String UNAVAILABLE = "El asistente no está disponible en este momento";

    private final ConversationRepository conversations;
    private final AssistantMessageRepository messages;
    private final AssistantModel model;
    private final UserRepository users;
    private final Clock clock;

    @Inject
    public AssistantUseCase(ConversationRepository conversations, AssistantMessageRepository messages, AssistantModel model,
                            UserRepository users, Clock clock) {
        this.conversations = conversations;
        this.messages = messages;
        this.model = model;
        this.users = users;
        this.clock = clock;
    }

    public List<Conversation> list(UUID userId) {
        return conversations.findForUser(userId);
    }

    /** title opcional: si falta, se pone con la primera pregunta. */
    public Conversation create(UUID userId, String title) {
        String clean = title == null || title.isBlank() ? null : title.trim();
        return conversations.save(Conversation.builder().userId(userId).title(clean).build());
    }

    public ConversationDetail get(UUID userId, UUID id) {
        Conversation conversation = find(userId, id);
        return new ConversationDetail(conversation, messages.findByConversation(id));
    }

    public void delete(UUID userId, UUID id) {
        Conversation conversation = find(userId, id);
        conversation.delete(clock.instant());
        conversations.save(conversation);
    }

    public AssistantMessage send(UUID userId, UUID id, String content) {
        return send(userId, id, content, AnswerListener.NONE);
    }

    /**
     * Guarda la pregunta, le pide la respuesta al modelo y la guarda. Si el modelo falla, la pregunta queda guardada:
     * reintentar la misma pregunta no la repite.
     */
    public AssistantMessage send(UUID userId, UUID id, String content, AnswerListener listener) {
        if (!model.available()) {
            throw new UnavailableException(UNAVAILABLE);
        }
        Conversation conversation = find(userId, id);
        String question = content.trim();
        List<AssistantMessage> last = messages.findLast(id, 1);
        boolean retry = !last.isEmpty() && last.getFirst().getRole() == MessageRole.USER && last.getFirst().getContent().equals(question);
        if (!retry) {
            if (messages.countQuestionsSince(userId, clock.instant().minus(Duration.ofHours(1))) >= QUESTIONS_PER_HOUR) {
                throw new TooManyRequestsException(TOO_MANY);
            }
            messages.save(AssistantMessage.builder().conversationId(id).role(MessageRole.USER).content(question).metadata(new HashMap<>()).build());
        }
        if (conversation.getTitle() == null) {
            conversation.setTitle(question.length() > TITLE_LENGTH ? question.substring(0, TITLE_LENGTH - 1) + "…" : question);
        }
        conversation.setUpdatedAt(clock.instant()); // la sube en la lista
        conversations.save(conversation);

        ZoneId zone = users.findById(userId).map(User::getTimezone).map(ZoneId::of).orElse(clock.getZone());
        Answer answer = model.answer(userId, LocalDate.now(clock.withZone(zone)), zone, messages.findLast(id, HISTORY), listener);
        return messages.save(AssistantMessage.builder()
                .conversationId(id)
                .role(MessageRole.ASSISTANT)
                .content(answer.text())
                .metadata(new HashMap<>(Map.of(METADATA_REFERENCES, answer.references(), METADATA_TOOLS, answer.tools(), METADATA_TOKENS, answer.tokens())))
                .build());
    }

    private Conversation find(UUID userId, UUID id) {
        return conversations.findByIdForUser(userId, id).orElseThrow(() -> new NotFoundException("conversación"));
    }
}
