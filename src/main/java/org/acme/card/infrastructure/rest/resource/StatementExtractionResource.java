package org.acme.card.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.card.application.model.StatementConfirmation;
import org.acme.card.application.model.StatementReview;
import org.acme.card.application.usecase.StatementExtractionUseCase;
import org.acme.card.infrastructure.rest.dto.CardStatementResponse;
import org.acme.card.infrastructure.rest.mapper.CardRestMapper;
import org.acme.document.application.usecase.ExtractionReviews;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;
import java.util.UUID;

/** Revisión de lo que leyó la IA (CU-004, CU-015). Nada queda confirmado sin el usuario (RN-013). */
@Path("/extractions")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "IA documental")
public class StatementExtractionResource {

    @Inject
    StatementExtractionUseCase extractions;

    @Inject
    ExtractionReviews reviews;

    @Inject
    CardRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Última propuesta de un documento",
            description = "Devuelve su id y su esquema (card_statement/1 o invoice/1); 404 si la IA todavía no lo leyó")
    public Map<String, String> latestForDocument(@QueryParam("documentId") @NotNull(message = "Indicá el documento") UUID documentId) {
        return reviews.latestForDocument(currentUser.id(), documentId)
                .map(r -> Map.of("id", r.getId().toString(), "schemaVersion", r.getSchemaVersion()))
                .orElseThrow(() -> new NotFoundException("propuesta"));
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Revisar una propuesta de resumen", description = "Datos leídos, tarjeta sugerida, avisos de los controles y totales")
    public StatementReview review(@PathParam("id") UUID id) {
        return extractions.review(currentUser.id(), id);
    }

    @POST
    @Path("/{id}/confirm")
    @Operation(summary = "Confirmar el resumen",
            description = "Crea el resumen, sus vencimientos y sus movimientos con cuotas. Mismas validaciones que la carga manual")
    public CardStatementResponse confirm(@PathParam("id") UUID id, @NotNull StatementConfirmation confirmation) {
        return mapper.toResponse(extractions.confirm(currentUser.id(), id, confirmation));
    }

    @POST
    @Path("/{id}/reject")
    @Operation(summary = "Descartar la propuesta", description = "No se crea nada; el documento queda guardado")
    public Response reject(@PathParam("id") UUID id) {
        extractions.reject(currentUser.id(), id);
        return Response.noContent().build();
    }
}
