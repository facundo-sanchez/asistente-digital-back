package org.acme.obligation.infrastructure.rest.mapper;

import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.application.model.ObligationView;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.shared.domain.model.ServiceCategory;
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
    ObligationResponse toResponse(ObligationSummary summary, String serviceName, ServiceCategory serviceCategory, boolean serviceAutoDebit);

    default ObligationResponse toResponse(ObligationView view) {
        return toResponse(view.summary(), view.serviceName(), view.serviceCategory(), view.serviceAutoDebit());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "invoiceId", ignore = true)
    @Mapping(target = "recurrenceId", ignore = true)
    @Mapping(target = "loanId", ignore = true)
    @Mapping(target = "installmentNumber", ignore = true)
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

    @Mapping(target = "amount", source = "estimatedAmount")
    RecurringRuleResponse toResponse(RecurringRule rule);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "assetId", ignore = true)
    @Mapping(target = "dayOfMonth", ignore = true)
    @Mapping(target = "estimatedAmount", source = "amount")
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RecurringRule toModel(RecurringRuleRequest request);
}
