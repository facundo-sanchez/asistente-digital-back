package org.acme.loan.infrastructure.persistence.mapper;

import org.acme.loan.domain.model.Loan;
import org.acme.loan.infrastructure.persistence.entity.LoanEntity;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface LoanPersistenceMapper {
    Loan toModel(LoanEntity entity);
    LoanEntity toEntity(Loan model);
}
