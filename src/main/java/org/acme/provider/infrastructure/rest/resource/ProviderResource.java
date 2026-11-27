package org.acme.provider.infrastructure.rest.resource;

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
import org.acme.provider.application.usecase.CreateProviderUseCase;
import org.acme.provider.application.usecase.ProviderQueryUseCase;
import org.acme.provider.infrastructure.rest.dto.ProviderRequest;
import org.acme.provider.infrastructure.rest.dto.ProviderResponse;
import org.acme.provider.infrastructure.rest.mapper.ProviderRestMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.domain.model.ServiceCategory;
import org.acme.shared.infrastructure.rest.EnumParams;
import org.acme.shared.infrastructure.rest.dto.PageResponse;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/** Catálogo de proveedores visible para el usuario (globales más propios) y alta de proveedores propios. */
@Path("/providers")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Proveedores")
public class ProviderResource {

    @Inject
    ProviderQueryUseCase providers;

    @Inject
    CreateProviderUseCase createProvider;

    @Inject
    ProviderRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Listar proveedores", description = "Catálogo y proveedores propios, activos, ordenados por nombre")
    public PageResponse<ProviderResponse> list(
            @QueryParam("query") @Size(max = 100, message = "La búsqueda admite hasta 100 caracteres")
            @Parameter(description = "Parte del nombre (sin distinguir mayúsculas)", example = "coope") String query,
            @QueryParam("category")
            @Parameter(description = "Categoría exacta (ELECTRICITY, GAS, WATER…)", example = "ELECTRICITY") String category,
            @QueryParam("page") @DefaultValue("0") @Min(value = 0, message = "La página empieza en 0") int page,
            @QueryParam("size") @DefaultValue("20")
            @Min(value = 1, message = "El tamaño mínimo es 1")
            @Max(value = PageQuery.MAX_SIZE, message = "El tamaño máximo es 100") int size) {
        return PageResponse.from(
                providers.list(currentUser.id(), query, EnumParams.parse(ServiceCategory.class, category, "Categoría inválida"), new PageQuery(page, size)),
                mapper::toResponse);
    }


    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar un proveedor", description = "404 si no existe o es propio de otro usuario")
    public ProviderResponse get(@PathParam("id") UUID id) {
        return mapper.toResponse(providers.get(currentUser.id(), id));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Crear un proveedor propio",
            description = "Para cuando no está en el catálogo. Solo lo ve su dueño. 409 si ya existe uno con ese nombre")
    public Response create(@Valid @NotNull ProviderRequest request) {
        ProviderResponse body = mapper.toResponse(createProvider.create(currentUser.id(), mapper.toModel(request)));
        return Response.status(Response.Status.CREATED).entity(body).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Editar un proveedor propio", description = "Nombre, categoría, sitio y remitente de facturas. 409 si es del catálogo")
    public ProviderResponse update(@PathParam("id") UUID id, @Valid @NotNull ProviderRequest request) {
        return mapper.toResponse(createProvider.update(currentUser.id(), id, mapper.toModel(request)));
    }
}
