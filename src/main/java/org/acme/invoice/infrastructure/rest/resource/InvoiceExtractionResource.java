package org.acme.invoice.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.invoice.application.model.InvoiceConfirmation;
import org.acme.invoice.application.model.InvoiceReview;
import org.acme.invoice.application.usecase.InvoiceExtractionUseCase;
import org.acme.invoice.infrastructure.rest.dto.InvoiceResponse;
import org.acme.invoice.infrastructure.rest.mapper.InvoiceRestMapper;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/** Revisión de facturas leídas por la IA (CU-002, CU-004). Nada queda confirmado sin el usuario (RN-013). */
@Path("/invoice-extractions")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "IA documental")
public class InvoiceExtractionResource {

    @Inject
    InvoiceExtractionUseCase extractions;

    @Inject
    InvoiceRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Path("/{id}")
    @Operation(summary = "Revisar una factura leída por la IA", description = "Datos leídos, servicio sugerido y avisos")
    public InvoiceReview review(@PathParam("id") UUID id) {
        return extractions.review(currentUser.id(), id);
    }

    @POST
    @Path("/{id}/confirm")
    @Operation(summary = "Confirmar la factura", description = "Queda confirmada y genera su vencimiento. Mismas validaciones que la carga manual")
    public InvoiceResponse confirm(@PathParam("id") UUID id, @NotNull InvoiceConfirmation confirmation) {
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
