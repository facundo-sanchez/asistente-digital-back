package org.acme.loan.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loan.application.model.LoanView;
import org.acme.loan.application.model.NewLoan;
import org.acme.loan.application.usecase.LoanUseCase;
import org.acme.loan.domain.model.Loan;
import org.acme.loan.infrastructure.rest.dto.*;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.obligation.infrastructure.rest.dto.ObligationResponse;
import org.acme.obligation.infrastructure.rest.mapper.ObligationRestMapper;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/** Préstamos tomados (módulo 13). Las cuotas son vencimientos: se pagan desde su vencimiento. */
@Path("/loans")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Préstamos")
public class LoanResource {

    @Inject
    LoanUseCase loans;

    @Inject
    ObligationRestMapper obligations;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Préstamos", description = "Con cuotas pagadas, lo que falta y la próxima cuota")
    public List<LoanResponse> list() {
        return loans.list(currentUser.id()).stream().map(v -> toResponse(v, false)).toList();
    }

    @POST
    @Operation(summary = "Cargar un préstamo", description = "Crea el préstamo y sus cuotas como vencimientos; las ya pagadas quedan pagadas")
    public Response create(@Valid @NotNull LoanRequest r) {
        Loan loan = Loan.builder().lender(r.lender()).name(r.name()).loanNumber(r.loanNumber()).shortReference(r.shortReference())
                .currency(r.currency()).principal(r.principal()).rateType(r.rateType()).annualRate(r.annualRate())
                .disbursementDate(r.disbursementDate()).installmentsCount(r.installmentsCount()).firstDueDate(r.firstDueDate())
                .contractDocumentId(r.contractDocumentId()).notes(r.notes()).build();
        LoanView view = loans.create(currentUser.id(), new NewLoan(loan, r.installmentAmount(), r.paidInstallments()));
        return Response.status(Response.Status.CREATED).entity(toResponse(view, true)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Ficha del préstamo con su cronograma")
    public LoanResponse get(@PathParam("id") UUID id) {
        return toResponse(loans.get(currentUser.id(), id), true);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Editar datos del préstamo", description = "No cambia cuotas ni montos")
    public LoanResponse update(@PathParam("id") UUID id, @Valid @NotNull LoanUpdateRequest r) {
        Loan changes = Loan.builder().lender(r.lender()).name(r.name()).loanNumber(r.loanNumber()).shortReference(r.shortReference())
                .principal(r.principal()).rateType(r.rateType()).annualRate(r.annualRate()).disbursementDate(r.disbursementDate())
                .contractDocumentId(r.contractDocumentId()).notes(r.notes()).build();
        return toResponse(loans.update(currentUser.id(), id, changes), true);
    }

    @PATCH
    @Path("/{id}/installments/{number}")
    @Operation(summary = "Cambiar el importe de una cuota pendiente", description = "Una cuota con pagos no se edita")
    public LoanResponse changeInstallment(@PathParam("id") UUID id, @PathParam("number") int number, @Valid @NotNull InstallmentAmountRequest r) {
        return toResponse(loans.changeInstallment(currentUser.id(), id, number, r.amount()), true);
    }

    @POST
    @Path("/{id}/cancel")
    @Operation(summary = "Cancelar el préstamo", description = "Cancela las cuotas pendientes sin pagos; las pagadas quedan")
    public LoanResponse cancel(@PathParam("id") UUID id, @Valid CancelLoanRequest r) {
        return toResponse(loans.cancel(currentUser.id(), id, r == null ? null : r.reason()), true);
    }

    private LoanResponse toResponse(LoanView v, boolean withInstallments) {
        Loan l = v.loan();
        List<ObligationResponse> installments = withInstallments ? v.installments().stream().map(this::installment).toList() : List.of();
        return new LoanResponse(l.getId(), l.getLender(), l.getName(), l.getLoanNumber(), l.getShortReference(), l.getCurrency(), l.getPrincipal(),
                l.getRateType(), l.getAnnualRate(), l.getDisbursementDate(), l.getInstallmentsCount(), l.getFirstDueDate(), l.getContractDocumentId(),
                l.getBankBalance(), l.getBankBalanceDate(), l.getNotes(), v.effectiveStatus(), l.getCancelReason(), v.paidCount(), v.remaining(),
                v.totalPaid(), v.next().map(this::installment).orElse(null),
                v.installments().isEmpty() ? null : v.installments().getLast().obligation().getDueDate(), installments, l.getCreatedAt());
    }

    private ObligationResponse installment(ObligationSummary s) {
        return obligations.toResponse(s, null, null);
    }
}
