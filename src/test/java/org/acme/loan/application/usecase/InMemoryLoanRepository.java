package org.acme.loan.application.usecase;

import org.acme.loan.application.port.output.LoanRepository;
import org.acme.loan.domain.model.Loan;

import java.util.*;

public class InMemoryLoanRepository implements LoanRepository {

    private final Map<UUID, Loan> rows = new LinkedHashMap<>();

    @Override
    public Loan save(Loan loan) {
        Loan copy = loan.toBuilder().id(loan.getId() == null ? UUID.randomUUID() : loan.getId()).build();
        rows.put(copy.getId(), copy);
        return copy.toBuilder().build();
    }

    @Override
    public Optional<Loan> findByIdForUser(UUID userId, UUID loanId) {
        return Optional.ofNullable(rows.get(loanId)).filter(l -> l.getUserId().equals(userId)).map(l -> l.toBuilder().build());
    }

    @Override
    public List<Loan> findForUser(UUID userId) {
        return rows.values().stream().filter(l -> l.getUserId().equals(userId)).toList();
    }

    @Override
    public Optional<Loan> findByNumber(UUID userId, String loanNumber) {
        return rows.values().stream().filter(l -> l.getUserId().equals(userId) && loanNumber.equals(l.getLoanNumber())).findFirst();
    }

    @Override
    public boolean existsNumber(UUID userId, String loanNumber, UUID excludeId) {
        return rows.values().stream().anyMatch(l -> l.getUserId().equals(userId) && loanNumber.equals(l.getLoanNumber()) && !l.getId().equals(excludeId));
    }
}
