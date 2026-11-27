package org.acme.loan.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.loan.application.port.output.LoanRepository;
import org.acme.loan.domain.model.Loan;
import org.acme.loan.domain.model.LoanStatus;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.obligation.domain.model.ObligationSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Aviso de cuota por email (módulo 13.5): busca el préstamo por la referencia corta del banco (ej. "203/2"); si todavía
 * no la tiene y el usuario tiene un solo préstamo activo de esa entidad, lo asocia y la guarda. Pone el importe real y la
 * fecha en la cuota, que deja de ser estimada. No toca cuotas con pagos.
 */
@ApplicationScoped
public class LoanNoticeUseCase {

    public static final String NOT_FOUND = "LOAN_NOT_FOUND";
    public static final String AMBIGUOUS = "LOAN_AMBIGUOUS";
    public static final String PAID = "LOAN_INSTALLMENT_PAID";

    private final LoanRepository loans;
    private final ObligationRepository obligations;

    @Inject
    public LoanNoticeUseCase(LoanRepository loans, ObligationRepository obligations) {
        this.loans = loans;
        this.obligations = obligations;
    }

    /** null si se aplicó; si no, el motivo. lender: el nombre de la entidad que mandó el aviso (ej. "BBVA"). */
    @Transactional
    public String apply(UUID userId, String lender, int number, String reference, BigDecimal amount, LocalDate dueDate) {
        List<Loan> active = loans.findForUser(userId).stream().filter(l -> l.getStatus() == LoanStatus.ACTIVE).toList();
        Optional<Loan> loan = active.stream().filter(l -> reference.equalsIgnoreCase(l.getShortReference())).findFirst();
        if (loan.isEmpty()) {
            String name = lender == null ? "" : lender.toLowerCase(Locale.ROOT);
            List<Loan> fromLender = active.stream()
                    .filter(l -> l.getShortReference() == null && l.getLender() != null && l.getLender().toLowerCase(Locale.ROOT).contains(name))
                    .toList();
            if (fromLender.isEmpty()) return NOT_FOUND;
            if (fromLender.size() > 1) return AMBIGUOUS;
            Loan only = fromLender.getFirst();
            only.setShortReference(reference);
            loan = Optional.of(loans.save(only));
        }
        Optional<ObligationSummary> installment = obligations.findByLoan(userId, loan.get().getId()).stream()
                .filter(i -> i.obligation().getInstallmentNumber() != null && i.obligation().getInstallmentNumber() == number).findFirst();
        if (installment.isEmpty()) return NOT_FOUND;
        if (installment.get().paidAmount().signum() > 0 || installment.get().obligation().getStatus() != ObligationStatus.PENDING) {
            return PAID;
        }
        Obligation obligation = installment.get().obligation();
        obligation.setAmount(amount);
        obligation.setDueDate(dueDate);
        obligation.setEstimated(false);
        obligations.save(obligation);
        return null;
    }
}
