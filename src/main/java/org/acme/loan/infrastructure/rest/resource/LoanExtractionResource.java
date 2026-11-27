package org.acme.loan.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.acme.loan.application.model.LoanContractReview;
import org.acme.loan.application.model.LoanReceiptConfirmation;
import org.acme.loan.application.model.LoanReceiptReview;
import org.acme.loan.application.usecase.LoanExtractionUseCase;
import org.acme.loan.infrastructure.rest.dto.LoanReceiptConfirmationRequest;
import org.acme.loan.infrastructure.rest.dto.LoanResponse;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/** Comprobantes de préstamo leídos con IA (módulo 13.3): el de alta y el de pago de una cuota. */
@Path("/loan-extractions")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Préstamos")
public class LoanExtractionResource {

    @Inject
    LoanExtractionUseCase extractions;

    @Inject
    LoanResource loans;

    @Inject
    CurrentUser currentUser;

    @GET
    @Path("/contract/{id}")
    @Operation(summary = "Comprobante de alta leído", description = "Para completar el alta: POST /loans con extractionId")
    public LoanContractReview contract(@PathParam("id") UUID id) {
        return extractions.contractReview(currentUser.id(), id);
    }

    @GET
    @Path("/receipt/{id}")
    @Operation(summary = "Comprobante de pago leído", description = "Con el préstamo que le corresponde por número y los avisos")
    public LoanReceiptReview receipt(@PathParam("id") UUID id) {
        return extractions.receiptReview(currentUser.id(), id);
    }

    @POST
    @Path("/receipt/{id}/confirm")
    @Operation(summary = "Confirmar el comprobante de pago", description = "Fija el importe de la cuota, registra el pago con el comprobante y el saldo del banco")
    public LoanResponse confirm(@PathParam("id") UUID id, @Valid @NotNull LoanReceiptConfirmationRequest r) {
        return loans.toResponse(extractions.confirmReceipt(currentUser.id(), id,
                new LoanReceiptConfirmation(r.loanId(), r.installmentNumber(), r.amount(), r.paymentDate(), r.newBalance())), true);
    }

    @POST
    @Path("/{id}/reject")
    @Operation(summary = "Descartar la propuesta", description = "No se registra nada; el documento queda guardado")
    public void reject(@PathParam("id") UUID id) {
        extractions.reject(currentUser.id(), id);
    }
}
