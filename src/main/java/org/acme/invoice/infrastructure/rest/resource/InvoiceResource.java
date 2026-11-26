package org.acme.invoice.infrastructure.rest.resource;

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
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domain.exceptions.ServiceException;
import org.acme.invoice.application.model.InvoiceFilter;
import org.acme.invoice.application.usecase.InvoiceQueryUseCase;
import org.acme.invoice.application.usecase.RegisterInvoiceUseCase;
import org.acme.invoice.domain.model.InvoiceStatus;
import org.acme.invoice.infrastructure.rest.dto.InvoiceRequest;
import org.acme.invoice.infrastructure.rest.dto.InvoiceResponse;
import org.acme.invoice.infrastructure.rest.mapper.InvoiceRestMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.infrastructure.rest.EnumParams;
import org.acme.shared.infrastructure.rest.dto.PageResponse;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Facturas del usuario (CU-002). El dueño sale siempre del token. */
@Path("/invoices")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Facturas")
public class InvoiceResource {

    @Inject
    RegisterInvoiceUseCase registerInvoice;

    @Inject
    InvoiceQueryUseCase invoiceQuery;

    @Inject
    InvoiceRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Listar facturas", description = "Por vencimiento, más nuevas primero. query busca en el número")
    public PageResponse<InvoiceResponse> list(
            @QueryParam("query") @Size(max = 100, message = "La búsqueda admite hasta 100 caracteres") String query,
            @QueryParam("serviceId") UUID serviceId,
            @QueryParam("status") @Parameter(example = "CONFIRMED") String status,
            @QueryParam("from") @Parameter(description = "Vence desde (inclusive)") LocalDate from,
            @QueryParam("to") @Parameter(description = "Vence hasta (inclusive)") LocalDate to,
            @QueryParam("page") @DefaultValue("0") @Min(value = 0, message = "La página empieza en 0") int page,
            @QueryParam("size") @DefaultValue("20")
            @Min(value = 1, message = "El tamaño mínimo es 1")
            @Max(value = PageQuery.MAX_SIZE, message = "El tamaño máximo es 100") int size) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new ServiceException("La fecha hasta no puede ser anterior a la fecha desde");
        }
        var filter = new InvoiceFilter(query, serviceId, EnumParams.parse(InvoiceStatus.class, status, "Estado inválido"), from, to);
        return PageResponse.from(invoiceQuery.list(currentUser.id(), filter, new PageQuery(page, size)), mapper::toResponse);
    }

    @GET
    @Path("/duplicates")
    @Operation(summary = "Posibles facturas repetidas", description = "Confirmadas del servicio con el mismo número, o del mismo período e importe")
    public List<InvoiceResponse> duplicates(
            @QueryParam("serviceId") @NotNull(message = "El servicio es obligatorio") UUID serviceId,
            @QueryParam("invoiceNumber") @Size(max = 120) String invoiceNumber,
            @QueryParam("billingPeriod") @Size(max = 30) String billingPeriod,
            @QueryParam("totalAmount") BigDecimal totalAmount,
            @QueryParam("excludeId") UUID excludeId) {
        return invoiceQuery.duplicates(currentUser.id(), serviceId, invoiceNumber, billingPeriod, totalAmount, excludeId)
                .stream().map(mapper::toResponse).toList();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar una factura", description = "404 si no existe o es de otro usuario")
    public InvoiceResponse get(@PathParam("id") UUID id) {
        return mapper.toResponse(invoiceQuery.get(currentUser.id(), id));
    }

    @POST
    @Operation(summary = "Cargar una factura",
            description = "Queda confirmada y genera su vencimiento. 409 si el servicio ya tiene una factura con ese número")
    public Response create(@Valid @NotNull InvoiceRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(mapper.toResponse(registerInvoice.register(currentUser.id(), mapper.toModel(request))))
                .build();
    }
}
