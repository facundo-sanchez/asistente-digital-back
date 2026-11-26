package org.acme.card.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.card.infrastructure.persistence.entity.CardMerchantRuleEntity;

import java.util.UUID;

@ApplicationScoped
public class CardMerchantRulePanacheRepository implements PanacheRepositoryBase<CardMerchantRuleEntity, UUID> {
}
