package org.acme.loan.infrastructure.rest.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.loan.application.model.LoanView;
import org.acme.loan.domain.model.Loan;
import org.acme.loan.infrastructure.rest.dto.LoanResponse;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.obligation.infrastructure.rest.dto.ObligationResponse;
import org.acme.obligation.infrastructure.rest.mapper.ObligationRestMapper;

import java.util.List;

/** Arma la respuesta de un préstamo con lo que sale de sus cuotas; installments solo con el detalle. */
@ApplicationScoped
public class LoanResponseMapper {

    @Inject
    ObligationRestMapper obligations;

    public LoanResponse toResponse(LoanView v, boolean withInstallments) {
        Loan l = v.loan();
        List<ObligationResponse> installments = withInstallments ? v.installments().stream().map(this::installment).toList() : List.of();
        return new LoanResponse(l.getId(), l.getLender(), l.getName(), l.getLoanNumber(), l.getShortReference(), l.getCurrency(), l.getPrincipal(),
                l.getRateType(), l.getAnnualRate(), l.getDisbursementDate(), l.getInstallmentsCount(), l.getFirstDueDate(), l.getContractDocumentId(),
                l.getBankBalance(), l.getBankBalanceDate(), l.getNotes(), v.effectiveStatus(), l.getCancelReason(), v.paidCount(), v.remaining(),
                v.totalPaid(), v.next().map(this::installment).orElse(null),
                v.installments().isEmpty() ? null : v.installments().getLast().obligation().getDueDate(), installments,
                withInstallments ? v.schedule() : List.of(), l.getCreatedAt());
    }

    private ObligationResponse installment(ObligationSummary s) {
        return obligations.toResponse(s, null, null, false);
    }
}
