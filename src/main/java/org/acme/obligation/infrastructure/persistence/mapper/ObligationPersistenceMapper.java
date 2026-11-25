package org.acme.obligation.infrastructure.persistence.mapper;

import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.obligation.domain.model.RecurringRule;
import org.acme.obligation.infrastructure.persistence.entity.ObligationEntity;
import org.acme.obligation.infrastructure.persistence.entity.ObligationViewEntity;
import org.acme.obligation.infrastructure.persistence.entity.RecurringRuleEntity;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface ObligationPersistenceMapper {

    Obligation toModel(ObligationEntity entity);
    ObligationEntity toEntity(Obligation model);

    /** Lectura desde v_obligations. */
    @Mapping(target = "obligation", source = "entity")
    ObligationSummary toSummary(ObligationViewEntity entity);

    Obligation viewToModel(ObligationViewEntity entity);

    RecurringRule toModel(RecurringRuleEntity entity);
    RecurringRuleEntity toEntity(RecurringRule model);
}
