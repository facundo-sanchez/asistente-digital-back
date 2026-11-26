package org.acme.card.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.card.application.usecase.ManageCardStatementUseCase;
import org.acme.card.infrastructure.rest.dto.CardStatementRequest;
import org.acme.card.infrastructure.rest.dto.CardStatementResponse;
import org.acme.card.infrastructure.rest.mapper.CardRestMapper;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/** Resúmenes de tarjeta (CU-015). El dueño sale siempre del token. */
@Path("/card-statements")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Tarjetas")
public class CardStatementResource {

    @Inject
    ManageCardStatementUseCase manageStatement;

    @Inject
    CardRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @POST
    @Operation(summary = "Cargar un resumen",
            description = "Queda confirmado y genera un vencimiento por moneda con saldo. 409 si la tarjeta ya tiene un resumen con ese vencimiento")
    public Response create(@Valid @NotNull CardStatementRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(mapper.toResponse(manageStatement.register(currentUser.id(), mapper.toModel(request))))
                .build();
    }

    @GET
    @Operation(summary = "Buscar el resumen de un vencimiento", description = "404 si el vencimiento no salió de un resumen")
    public CardStatementResponse byObligation(@QueryParam("obligationId") @NotNull(message = "Indicá el vencimiento") UUID obligationId) {
        return manageStatement.findByObligation(currentUser.id(), obligationId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("resumen"));
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar un resumen", description = "404 si es de otro usuario")
    public CardStatementResponse get(@PathParam("id") UUID id) {
        return mapper.toResponse(manageStatement.get(currentUser.id(), id));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Corregir un resumen", description = "Copia los cambios a sus vencimientos. 400 si tienen pagos")
    public CardStatementResponse update(@PathParam("id") UUID id, @Valid @NotNull CardStatementRequest request) {
        return mapper.toResponse(manageStatement.update(currentUser.id(), id, mapper.toModel(request)));
    }

    @POST
    @Path("/{id}/reject")
    @Operation(summary = "Anular un resumen", description = "Cancela sus vencimientos. 400 si tienen pagos")
    public Response reject(@PathParam("id") UUID id) {
        manageStatement.reject(currentUser.id(), id);
        return Response.noContent().build();
    }
}
