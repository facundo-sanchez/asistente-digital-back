package org.acme.payment.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.payment.application.usecase.RegisterPaymentUseCase;
import org.acme.payment.infrastructure.rest.dto.PaymentRequest;
import org.acme.payment.infrastructure.rest.mapper.PaymentRestMapper;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

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
}
