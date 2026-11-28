package org.acme.feedback.infrastructure.rest;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.acme.feedback.application.usecase.FeedbackUseCase;
import org.acme.feedback.infrastructure.rest.dto.FeedbackRequest;
import org.acme.feedback.infrastructure.rest.dto.FeedbackResponse;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/** Comentarios, sugerencias y reportes de errores del usuario. */
@Path("/feedback")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Comentarios")
public class FeedbackResource {

    @Inject
    FeedbackUseCase feedback;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Lo que mandé", description = "Los últimos 20, lo más nuevo primero")
    public List<FeedbackResponse> list() {
        return feedback.recent(currentUser.id()).stream().map(FeedbackResponse::from).toList();
    }

    @POST
    @Operation(summary = "Mandar un comentario, sugerencia o reporte de error")
    public FeedbackResponse send(@Valid @NotNull FeedbackRequest request, @HeaderParam(HttpHeaders.USER_AGENT) String userAgent) {
        return FeedbackResponse.from(feedback.send(currentUser.id(), request.kind(), request.message(), request.page(), request.details(), userAgent));
    }
}
