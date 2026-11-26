package org.acme.provider.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.provider.infrastructure.persistence.entity.ProviderEmailRuleEntity;

import java.util.UUID;

@ApplicationScoped
public class ProviderEmailRulePanacheRepository implements PanacheRepositoryBase<ProviderEmailRuleEntity, UUID> {
}
