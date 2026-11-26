package org.acme.obligation.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.obligation.application.usecase.ManageRecurringRuleUseCase;
import org.acme.obligation.infrastructure.rest.dto.RecurringRuleRequest;
import org.acme.obligation.infrastructure.rest.dto.RecurringRuleResponse;
import org.acme.obligation.infrastructure.rest.dto.RecurringRuleStatusRequest;
import org.acme.obligation.infrastructure.rest.mapper.ObligationRestMapper;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/** Vencimientos que se repiten (alquiler, seguros, VTV). El dueño sale siempre del token. */
@Path("/recurring-rules")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Obligaciones")
public class RecurringRuleResource {

    @Inject
    ManageRecurringRuleUseCase recurringRules;

    @Inject
    ObligationRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Listar repeticiones")
    public List<RecurringRuleResponse> list() {
        return recurringRules.list(currentUser.id()).stream().map(mapper::toResponse).toList();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar una repetición", description = "404 si es de otro usuario")
    public RecurringRuleResponse get(@PathParam("id") UUID id) {
        return mapper.toResponse(recurringRules.get(currentUser.id(), id));
    }

    @POST
    @Operation(summary = "Crear una repetición", description = "Crea también el vencimiento de la primera fecha; los siguientes los genera una tarea diaria")
    public Response create(@Valid @NotNull RecurringRuleRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(mapper.toResponse(recurringRules.create(currentUser.id(), mapper.toModel(request))))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Editar una repetición", description = "Afecta a los próximos períodos; los vencimientos ya creados no cambian")
    public RecurringRuleResponse update(@PathParam("id") UUID id, @Valid @NotNull RecurringRuleRequest request) {
        return mapper.toResponse(recurringRules.update(currentUser.id(), id, mapper.toModel(request)));
    }

    @PATCH
    @Path("/{id}/status")
    @Operation(summary = "Activar o desactivar una repetición")
    public RecurringRuleResponse changeStatus(@PathParam("id") UUID id, @Valid @NotNull RecurringRuleStatusRequest request) {
        return mapper.toResponse(recurringRules.changeStatus(currentUser.id(), id, request.active()));
    }
}
