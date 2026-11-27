package org.acme.loan.application.model;

import org.acme.loan.domain.model.FrenchSchedule;
import org.acme.loan.domain.model.Loan;
import org.acme.loan.domain.model.LoanRateType;
import org.acme.loan.domain.model.ScheduleRow;
import org.acme.loan.domain.model.LoanStatus;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.obligation.domain.model.ObligationSummary;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Un préstamo con sus cuotas y lo que sale de ellas: cuántas se pagaron, cuánto falta y la próxima. */
public record LoanView(Loan loan, List<ObligationSummary> installments) {

    /** Cronograma calculado (tasa fija con capital y TNA); vacío si no se puede calcular. */
    public List<ScheduleRow> schedule() {
        return loan.getRateType() == LoanRateType.FIXED
                ? FrenchSchedule.of(loan.getPrincipal(), loan.getAnnualRate(), loan.getInstallmentsCount()) : List.of();
    }

    public long paidCount() {
        return installments.stream().filter(i -> i.effectiveStatus() == EffectiveObligationStatus.PAID).count();
    }

    public List<ObligationSummary> pending() {
        return installments.stream().filter(LoanView::pending).toList();
    }

    /** Suma de lo que falta de las cuotas pendientes (estimado hasta que llega el importe real). */
    public BigDecimal remaining() {
        return pending().stream().map(i -> i.balance() == null ? BigDecimal.ZERO : i.balance()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalPaid() {
        return installments.stream().map(ObligationSummary::paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Optional<ObligationSummary> next() {
        return pending().stream().findFirst();
    }

    /** PAID cuando no quedan cuotas pendientes; si no, el estado guardado. */
    public String effectiveStatus() {
        if (loan.getStatus() == LoanStatus.CANCELLED) return LoanStatus.CANCELLED.name();
        return !installments.isEmpty() && pending().isEmpty() ? "PAID" : LoanStatus.ACTIVE.name();
    }

    private static boolean pending(ObligationSummary i) {
        return i.effectiveStatus() != EffectiveObligationStatus.PAID && i.effectiveStatus() != EffectiveObligationStatus.CANCELLED;
    }
}
