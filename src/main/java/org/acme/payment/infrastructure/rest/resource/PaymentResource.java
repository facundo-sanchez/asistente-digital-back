package org.acme.payment.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.payment.application.usecase.ManagePaymentUseCase;
import org.acme.payment.application.usecase.RegisterPaymentUseCase;
import org.acme.payment.infrastructure.rest.dto.PaymentRequest;
import org.acme.payment.infrastructure.rest.dto.PaymentResponse;
import org.acme.payment.infrastructure.rest.dto.VoidPaymentRequest;
import org.acme.payment.infrastructure.rest.mapper.PaymentRestMapper;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/** Pagos de obligaciones (CU-006). El dueño sale siempre del token. */
@Path("/payments")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Pagos")
public class PaymentResource {

    @Inject
    RegisterPaymentUseCase registerPayment;

    @Inject
    ManagePaymentUseCase managePayment;

    @Inject
    PaymentRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @POST
    @Operation(summary = "Registrar un pago",
            description = "Total o parcial. La moneda es la de la obligación. 400 si está pagada o cancelada")
    public Response create(@Valid @NotNull PaymentRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(mapper.toResponse(registerPayment.register(currentUser.id(), mapper.toModel(request))))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Corregir un pago", description = "Anula el pago y crea uno nuevo que lo reemplaza (el anulado queda en el historial)")
    public PaymentResponse correct(@PathParam("id") UUID id, @Valid @NotNull PaymentRequest request) {
        return mapper.toResponse(managePayment.correct(currentUser.id(), id, mapper.toModel(request)));
    }

    @POST
    @Path("/{id}/void")
    @Operation(summary = "Anular un pago", description = "Con motivo. El pago queda en el historial y la obligación recalcula su estado")
    public Response voidPayment(@PathParam("id") UUID id, @Valid @NotNull VoidPaymentRequest request) {
        managePayment.voidPayment(currentUser.id(), id, request.reason());
        return Response.noContent().build();
    }
}
