package org.acme.loan.infrastructure.persistence.repository;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.loan.application.port.output.LoanRepository;
import org.acme.loan.domain.model.Loan;
import org.acme.loan.infrastructure.persistence.entity.LoanEntity;
import org.acme.loan.infrastructure.persistence.mapper.LoanPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class LoanRepositoryAdapter implements LoanRepository {

    @Inject
    LoanPanacheRepository repository;

    @Inject
    LoanPersistenceMapper mapper;

    @Override
    @Transactional
    public Loan save(Loan loan) {
        LoanEntity entity = mapper.toEntity(loan);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    public Optional<Loan> findByIdForUser(UUID userId, UUID loanId) {
        return repository.find("id = ?1 and userId = ?2", loanId, userId).firstResultOptional().map(mapper::toModel);
    }

    @Override
    public List<Loan> findForUser(UUID userId) {
        return repository.list("userId", Sort.descending("createdAt"), userId).stream().map(mapper::toModel).toList();
    }

    @Override
    public Optional<Loan> findByNumber(UUID userId, String loanNumber) {
        return repository.find("userId = ?1 and loanNumber = ?2", userId, loanNumber).firstResultOptional().map(mapper::toModel);
    }

    @Override
    public boolean existsNumber(UUID userId, String loanNumber, UUID excludeId) {
        return excludeId == null
                ? repository.count("userId = ?1 and loanNumber = ?2", userId, loanNumber) > 0
                : repository.count("userId = ?1 and loanNumber = ?2 and id <> ?3", userId, loanNumber, excludeId) > 0;
    }
}
