package org.acme.card.infrastructure.rest.resource;

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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.card.application.model.CardView;
import org.acme.card.application.model.SpendingFilter;
import org.acme.card.application.usecase.CardReportUseCase;
import org.acme.card.application.usecase.ManageCardStatementUseCase;
import org.acme.card.application.usecase.ManageCardUseCase;
import org.acme.card.infrastructure.rest.dto.CardRequest;
import org.acme.card.infrastructure.rest.dto.CardResponse;
import org.acme.card.infrastructure.rest.dto.CardSpendingReportResponse;
import org.acme.card.infrastructure.rest.dto.PendingInstallmentsResponse;
import org.acme.card.infrastructure.rest.dto.CardStatementResponse;
import org.acme.card.infrastructure.rest.dto.CardStatusRequest;
import org.acme.card.infrastructure.rest.mapper.CardRestMapper;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Tarjetas de crédito del usuario (CU-015). El dueño sale siempre del token. */
@Path("/cards")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Tarjetas")
public class CardResource {

    @Inject
    ManageCardUseCase manageCard;

    @Inject
    ManageCardStatementUseCase manageStatement;

    @Inject
    CardReportUseCase report;

    @Inject
    CardRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Listar tarjetas", description = "Por nombre. active filtra activas o dadas de baja")
    public List<CardResponse> list(@QueryParam("active") Boolean active) {
        return manageCard.list(currentUser.id(), active).stream().map(this::toResponse).toList();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar una tarjeta", description = "404 si es de otro usuario")
    public CardResponse get(@PathParam("id") UUID id) {
        return toResponse(manageCard.get(currentUser.id(), id));
    }

    @GET
    @Path("/{id}/statements")
    @Operation(summary = "Resúmenes de una tarjeta", description = "Anulados incluidos, más nuevos primero")
    public List<CardStatementResponse> statements(@PathParam("id") UUID id) {
        return manageStatement.listForCard(currentUser.id(), id).stream().map(mapper::toResponse).toList();
    }

    @POST
    @Operation(summary = "Registrar una tarjeta",
            description = "El servicio tiene que ser de la categoría Tarjetas. 409 si ya hay una de esa marca y terminación")
    public Response create(@Valid @NotNull CardRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(manageCard.register(currentUser.id(), mapper.toModel(request))))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Editar una tarjeta", description = "Mismas validaciones que el alta. No cambia el estado")
    public CardResponse update(@PathParam("id") UUID id, @Valid @NotNull CardRequest request) {
        return toResponse(manageCard.update(currentUser.id(), id, mapper.toModel(request)));
    }

    @PATCH
    @Path("/{id}/status")
    @Operation(summary = "Activar o desactivar una tarjeta", description = "Desactivar no borra sus resúmenes ni vencimientos")
    public CardResponse changeStatus(@PathParam("id") UUID id, @Valid @NotNull CardStatusRequest request) {
        return toResponse(manageCard.changeStatus(currentUser.id(), id, request.active()));
    }

    private CardResponse toResponse(CardView view) {
        return mapper.toResponse(view.card(), view.serviceName());
    }

    @GET
    @Path("/report")
    @Operation(summary = "Informe de consumos", description = "Meses de facturación from/to (yyyy-MM-dd); sin período, los últimos 6. Por moneda (RN-018)")
    public CardSpendingReportResponse report(@QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to, @QueryParam("cardId") UUID cardId) {
        return mapper.toResponse(report.report(currentUser.id(), new SpendingFilter(from, to, cardId, null, null)));
    }

    @GET
    @Path("/installments/pending")
    @Operation(summary = "Cuotas pendientes", description = "Por compra: cuota actual, cuántas faltan y cuánto; total por moneda")
    public PendingInstallmentsResponse pendingInstallments() {
        return mapper.toResponse(report.pending(currentUser.id()));
    }
}
