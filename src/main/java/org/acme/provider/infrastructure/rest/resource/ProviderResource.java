package org.acme.provider.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.acme.provider.application.usecase.ProviderQueryUseCase;
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

/** Catálogo de proveedores visible para el usuario: los globales más los propios. */
@Path("/providers")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Proveedores")
public class ProviderResource {

    @Inject
    ProviderQueryUseCase providers;

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
                providers.list(currentUser.id(), query, EnumParams.parse(ServiceCategory.class, category, "Categoría"), new PageQuery(page, size)),
                mapper::toResponse);
    }


    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar un proveedor", description = "404 si no existe o es propio de otro usuario")
    public ProviderResponse get(@PathParam("id") UUID id) {
        return mapper.toResponse(providers.get(currentUser.id(), id));
    }
}
