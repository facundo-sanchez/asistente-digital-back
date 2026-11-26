package org.acme.obligation.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domain.exceptions.ServiceException;
import org.acme.obligation.application.model.ObligationFilter;
import org.acme.obligation.application.usecase.ManageObligationUseCase;
import org.acme.obligation.application.usecase.ObligationQueryUseCase;
import org.acme.obligation.application.usecase.RegisterObligationUseCase;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.obligation.infrastructure.rest.dto.CancelObligationRequest;
import org.acme.obligation.infrastructure.rest.dto.ObligationRequest;
import org.acme.obligation.infrastructure.rest.dto.ObligationResponse;
import org.acme.obligation.infrastructure.rest.mapper.ObligationRestMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.domain.model.ServiceCategory;
import org.acme.shared.infrastructure.rest.EnumParams;
import org.acme.shared.infrastructure.rest.dto.PageResponse;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Obligaciones y vencimientos del usuario (CU-005). El dueño sale siempre del token. */
@Path("/obligations")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Obligaciones")
public class ObligationResource {

    @Inject
    RegisterObligationUseCase registerObligation;

    @Inject
    ObligationQueryUseCase obligationQuery;

    @Inject
    ManageObligationUseCase manageObligation;

    @Inject
    ObligationRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Listar obligaciones", description = "Por vencimiento. status acepta varios estados separados por coma")
    public PageResponse<ObligationResponse> list(
            @QueryParam("query") @Size(max = 100, message = "La búsqueda admite hasta 100 caracteres") String query,
            @QueryParam("status") @Parameter(example = "PENDING,DUE_SOON,OVERDUE") String status,
            @QueryParam("serviceId") UUID serviceId,
            @QueryParam("category") @Parameter(example = "ELECTRICITY") String category,
            @QueryParam("from") @Parameter(description = "Vence desde (inclusive)", example = "2026-12-01") LocalDate from,
            @QueryParam("to") @Parameter(description = "Vence hasta (inclusive)", example = "2026-12-31") LocalDate to,
            @QueryParam("page") @DefaultValue("0") @Min(value = 0, message = "La página empieza en 0") int page,
            @QueryParam("size") @DefaultValue("20")
            @Min(value = 1, message = "El tamaño mínimo es 1")
            @Max(value = PageQuery.MAX_SIZE, message = "El tamaño máximo es 100") int size) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new ServiceException("La fecha hasta no puede ser anterior a la fecha desde");
        }
        var filter = new ObligationFilter(query, parseStatuses(status), serviceId,
                EnumParams.parse(ServiceCategory.class, category, "Categoría inválida"), from, to);
        return PageResponse.from(obligationQuery.list(currentUser.id(), filter, new PageQuery(page, size)), mapper::toResponse);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar una obligación", description = "404 si no existe o es de otro usuario")
    public ObligationResponse get(@PathParam("id") UUID id) {
        return mapper.toResponse(obligationQuery.get(currentUser.id(), id));
    }

    @POST
    @Operation(summary = "Registrar una obligación", description = "Alta manual (VTV, impuesto, seguro…). Importe y moneda (ARS o USD) van juntos")
    public Response create(@Valid @NotNull ObligationRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(mapper.toResponse(registerObligation.register(currentUser.id(), mapper.toModel(request))))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Editar una obligación", description = "Mismas validaciones que el alta. No si está pagada o cancelada")
    public ObligationResponse update(@PathParam("id") UUID id, @Valid @NotNull ObligationRequest request) {
        return mapper.toResponse(manageObligation.update(currentUser.id(), id, mapper.toModel(request)));
    }

    @POST
    @Path("/{id}/cancel")
    @Operation(summary = "Cancelar una obligación", description = "Motivo opcional. No si tiene pagos vigentes")
    public Response cancel(@PathParam("id") UUID id, @Valid CancelObligationRequest request) {
        manageObligation.cancel(currentUser.id(), id, request == null ? null : request.reason());
        return Response.noContent().build();
    }

    /** "PENDING,OVERDUE" → conjunto de estados. Vacío = sin filtro. */
    private static Set<EffectiveObligationStatus> parseStatuses(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(s -> EnumParams.parse(EffectiveObligationStatus.class, s, "Estado inválido"))
                .collect(Collectors.toUnmodifiableSet());
    }
}
