package org.acme.obligation.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.obligation.infrastructure.persistence.entity.RecurringRuleEntity;

import java.util.UUID;

@ApplicationScoped
public class RecurringRulePanacheRepository implements PanacheRepositoryBase<RecurringRuleEntity, UUID> {
}
