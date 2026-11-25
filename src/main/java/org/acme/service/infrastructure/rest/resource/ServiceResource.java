package org.acme.service.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.service.application.usecase.ManageServiceUseCase;
import org.acme.service.application.usecase.RegisterServiceUseCase;
import org.acme.service.application.usecase.ServiceQueryUseCase;
import org.acme.service.infrastructure.rest.dto.ServiceRequest;
import org.acme.service.infrastructure.rest.dto.ServiceResponse;
import org.acme.service.infrastructure.rest.dto.ServiceStatusRequest;
import org.acme.service.infrastructure.rest.mapper.ServiceRestMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.domain.model.ServiceCategory;
import org.acme.shared.infrastructure.rest.EnumParams;
import org.acme.shared.infrastructure.rest.dto.PageResponse;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/** Servicios del usuario autenticado (CU-001). El dueño sale siempre del token. */
@Path("/services")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Servicios")
public class ServiceResource {

    @Inject
    RegisterServiceUseCase registerService;

    @Inject
    ServiceQueryUseCase serviceQuery;

    @Inject
    ManageServiceUseCase manageService;

    @Inject
    ServiceRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @POST
    @Operation(summary = "Registrar un servicio", description = "Queda activo. El proveedor debe ser del catálogo o propio")
    public Response register(@Valid @NotNull ServiceRequest request) {
        ServiceResponse body = mapper.toResponse(registerService.register(currentUser.id(), mapper.toModel(request)));
        return Response.status(Response.Status.CREATED).entity(body).build();
    }

    @GET
    @Operation(summary = "Listar servicios", description = "Ordenados por nombre. query busca en el nombre y en el nro. de socio")
    public PageResponse<ServiceResponse> list(
            @QueryParam("query") @Size(max = 100, message = "La búsqueda admite hasta 100 caracteres")
            @Parameter(description = "Parte del nombre o del nro. de socio", example = "luz") String query,
            @QueryParam("category") @Parameter(description = "Categoría exacta", example = "ELECTRICITY") String category,
            @QueryParam("active") @Parameter(description = "true: activos, false: inactivos, vacío: todos") Boolean active,
            @QueryParam("page") @DefaultValue("0") @Min(value = 0, message = "La página empieza en 0") int page,
            @QueryParam("size") @DefaultValue("20")
            @Min(value = 1, message = "El tamaño mínimo es 1")
            @Max(value = PageQuery.MAX_SIZE, message = "El tamaño máximo es 100") int size) {
        return PageResponse.from(
                serviceQuery.list(currentUser.id(), query, EnumParams.parse(ServiceCategory.class, category, "Categoría inválida"),
                        active, new PageQuery(page, size)),
                mapper::toResponse);
    }

    @GET
    @Path("/duplicates")
    @Operation(summary = "Posibles duplicados",
            description = "Servicios propios con el mismo proveedor y nro. de socio (sin espacios, guiones ni mayúsculas). Para avisar antes de guardar")
    public List<ServiceResponse> duplicates(
            @QueryParam("providerId") @NotNull(message = "El proveedor es obligatorio") UUID providerId,
            @QueryParam("customerReference") @Size(max = 150) String customerReference,
            @QueryParam("excludeId") @Parameter(description = "El servicio que se está editando") UUID excludeId) {
        return serviceQuery.possibleDuplicates(currentUser.id(), providerId, customerReference, excludeId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar un servicio", description = "404 si no existe o es de otro usuario")
    public ServiceResponse get(@PathParam("id") UUID id) {
        return mapper.toResponse(serviceQuery.get(currentUser.id(), id));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Editar un servicio", description = "Mismas validaciones que el alta. No cambia el estado")
    public ServiceResponse update(@PathParam("id") UUID id, @Valid @NotNull ServiceRequest request) {
        return mapper.toResponse(manageService.update(currentUser.id(), id, mapper.toModel(request)));
    }

    @PATCH
    @Path("/{id}/status")
    @Operation(summary = "Activar o desactivar un servicio")
    public ServiceResponse changeStatus(@PathParam("id") UUID id, @Valid @NotNull ServiceStatusRequest request) {
        return mapper.toResponse(manageService.changeStatus(currentUser.id(), id, request.active()));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Dar de baja un servicio", description = "Baja lógica: lo desactiva y conserva su historial")
    public Response delete(@PathParam("id") UUID id) {
        manageService.delete(currentUser.id(), id);
        return Response.noContent().build();
    }
}
