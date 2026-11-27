package org.acme.loan.application.port.output;

import org.acme.loan.domain.model.Loan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository {

    Loan save(Loan loan);

    Optional<Loan> findByIdForUser(UUID userId, UUID loanId);

    /** Los del usuario, los más nuevos primero. */
    List<Loan> findForUser(UUID userId);

    /** Si ya tiene otro préstamo con ese número (excludeId: el que se está editando). */
    boolean existsNumber(UUID userId, String loanNumber, UUID excludeId);
}
