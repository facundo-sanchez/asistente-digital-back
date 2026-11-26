package org.acme.assistant.infrastructure.rest.resource;

import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.acme.assistant.application.model.AnswerListener;
import org.acme.assistant.application.usecase.AssistantUseCase;
import org.acme.assistant.infrastructure.rest.dto.AssistantStreamEvent;
import org.acme.assistant.infrastructure.rest.mapper.AssistantRestMapper;
import org.acme.domain.exceptions.DomainException;

import java.util.UUID;

/** Arma la respuesta en un hilo de trabajo y va emitiendo los eventos de la respuesta progresiva. */
@ApplicationScoped
public class AssistantStream {

    @Inject
    AssistantUseCase assistant;

    @Inject
    AssistantRestMapper mapper;

    /** Contexto de request propio: corre fuera del hilo del pedido HTTP. */
    @ActivateRequestContext
    public void run(UUID userId, UUID conversationId, String content, MultiEmitter<? super AssistantStreamEvent> emitter) {
        AnswerListener listener = new AnswerListener() {
            @Override
            public void tool(String name) {
                emitter.emit(AssistantStreamEvent.tool(name));
            }

            @Override
            public void text(String delta) {
                emitter.emit(AssistantStreamEvent.text(delta));
            }
        };
        try {
            emitter.emit(AssistantStreamEvent.done(mapper.toResponse(assistant.send(userId, conversationId, content, listener))));
        } catch (DomainException e) {
            emitter.emit(AssistantStreamEvent.error(e.getMessage()));
        } catch (RuntimeException e) {
            emitter.emit(AssistantStreamEvent.error("No pude responder ahora, probá de nuevo en un rato"));
        }
        emitter.complete();
    }
}
