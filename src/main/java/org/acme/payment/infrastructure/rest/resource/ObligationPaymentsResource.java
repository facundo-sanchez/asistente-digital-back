package org.acme.payment.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.payment.application.usecase.ManagePaymentUseCase;
import org.acme.payment.infrastructure.rest.dto.PaymentResponse;
import org.acme.payment.infrastructure.rest.mapper.PaymentRestMapper;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/** Historial de pagos de una obligación (RN-008). */
@Path("/obligations/{id}/payments")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Pagos")
public class ObligationPaymentsResource {

    @Inject
    ManagePaymentUseCase managePayment;

    @Inject
    PaymentRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Pagos de una obligación", description = "Anulados incluidos, más nuevos primero. 404 si la obligación es de otro usuario")
    public List<PaymentResponse> list(@PathParam("id") UUID obligationId) {
        return managePayment.history(currentUser.id(), obligationId).stream().map(mapper::toResponse).toList();
    }
}
