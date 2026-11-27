package org.acme.loan.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.loan.infrastructure.persistence.entity.LoanEntity;

import java.util.UUID;

@ApplicationScoped
public class LoanPanacheRepository implements PanacheRepositoryBase<LoanEntity, UUID> {
}
