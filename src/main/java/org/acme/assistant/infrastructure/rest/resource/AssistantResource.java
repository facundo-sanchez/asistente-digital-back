package org.acme.assistant.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.assistant.application.model.ConversationDetail;
import org.acme.assistant.application.usecase.AssistantUseCase;
import org.acme.assistant.infrastructure.rest.dto.AssistantStreamEvent;
import org.acme.assistant.infrastructure.rest.dto.ConversationDetailResponse;
import org.acme.assistant.infrastructure.rest.dto.ConversationResponse;
import org.acme.assistant.infrastructure.rest.dto.CreateConversationRequest;
import org.acme.assistant.infrastructure.rest.dto.MessageResponse;
import org.acme.assistant.infrastructure.rest.dto.SendMessageRequest;
import org.acme.assistant.infrastructure.rest.mapper.AssistantRestMapper;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.util.List;
import java.util.UUID;

/** Asistente conversacional (CU-008). */
@Path("/assistant/conversations")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Asistente")
public class AssistantResource {

    @Inject
    AssistantUseCase assistant;

    @Inject
    AssistantRestMapper mapper;

    @Inject
    AssistantStream stream;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Conversaciones", description = "La de actividad más reciente primero")
    public List<ConversationResponse> list() {
        return assistant.list(currentUser.id()).stream().map(mapper::toResponse).toList();
    }

    @POST
    @Operation(summary = "Nueva conversación")
    public Response create(@Valid CreateConversationRequest request) {
        String title = request == null ? null : request.title();
        return Response.status(Response.Status.CREATED).entity(mapper.toResponse(assistant.create(currentUser.id(), title))).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Conversación con sus mensajes")
    public ConversationDetailResponse get(@PathParam("id") UUID id) {
        ConversationDetail detail = assistant.get(currentUser.id(), id);
        return mapper.toDetail(detail.conversation(), detail.messages());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Borrar una conversación")
    public void delete(@PathParam("id") UUID id) {
        assistant.delete(currentUser.id(), id);
    }

    @POST
    @Path("/{id}/messages")
    @Operation(summary = "Preguntar", description = "Guarda la pregunta y devuelve la respuesta; metadata.references trae los registros usados")
    public MessageResponse send(@PathParam("id") UUID id, @Valid @NotNull SendMessageRequest request) {
        return mapper.toResponse(assistant.send(currentUser.id(), id, request.content()));
    }

    @POST
    @Path("/{id}/messages/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Operation(summary = "Preguntar con respuesta progresiva (SSE)",
            description = "Eventos tool, text y al final done (la respuesta guardada) o error")
    public Multi<AssistantStreamEvent> sendStream(@PathParam("id") UUID id, @Valid @NotNull SendMessageRequest request) {
        UUID userId = currentUser.id();
        return Multi.createFrom().<AssistantStreamEvent>emitter(emitter -> stream.run(userId, id, request.content(), emitter))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
