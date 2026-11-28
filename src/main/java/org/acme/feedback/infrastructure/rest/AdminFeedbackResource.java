package org.acme.feedback.infrastructure.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.acme.feedback.application.usecase.FeedbackUseCase;
import org.acme.feedback.domain.model.FeedbackKind;
import org.acme.feedback.infrastructure.rest.dto.AdminFeedbackResponse;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.infrastructure.rest.dto.PageResponse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Comentarios y reportes de todos los usuarios. Solo rol ADMIN: cualquier otro recibe 403. */
@Path("/admin/feedback")
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Administración")
public class AdminFeedbackResource {

    @Inject
    FeedbackUseCase feedback;

    @GET
    @Operation(summary = "Comentarios y reportes de todos", description = "Lo más nuevo primero; kind filtra por tipo")
    public PageResponse<AdminFeedbackResponse> list(@QueryParam("kind") FeedbackKind kind,
                                                    @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("20") int size) {
        return PageResponse.from(feedback.all(kind, new PageQuery(page, size)), AdminFeedbackResponse::from);
    }
}
