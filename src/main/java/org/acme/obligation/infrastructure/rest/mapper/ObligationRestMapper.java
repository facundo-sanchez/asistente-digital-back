package org.acme.obligation.infrastructure.rest.mapper;

import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.application.model.ObligationView;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.obligation.domain.model.RecurringRule;
import org.acme.obligation.infrastructure.rest.dto.ObligationRequest;
import org.acme.obligation.infrastructure.rest.dto.ObligationResponse;
import org.acme.obligation.infrastructure.rest.dto.RecurringRuleRequest;
import org.acme.obligation.infrastructure.rest.dto.RecurringRuleResponse;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.*;

@Mapper(config = MapStructConfig.class)
public interface ObligationRestMapper {

    @Mapping(target = ".", source = "summary.obligation")
    ObligationResponse toResponse(ObligationSummary summary, String serviceName);

    default ObligationResponse toResponse(ObligationView view) {
        return toResponse(view.summary(), view.serviceName());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "invoiceId", ignore = true)
    @Mapping(target = "recurrenceId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "estimated", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancelReason", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Obligation toModel(ObligationRequest request);

    @InheritConfiguration(name = "toModel")
    void update(ObligationRequest request, @MappingTarget Obligation obligation);

    RecurringRuleResponse toResponse(RecurringRule rule);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "active", source = "active", defaultValue = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RecurringRule toModel(RecurringRuleRequest request);

    @InheritConfiguration(name = "toModel")
    @Mapping(target = "active", source = "active", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(RecurringRuleRequest request, @MappingTarget RecurringRule rule);
}
