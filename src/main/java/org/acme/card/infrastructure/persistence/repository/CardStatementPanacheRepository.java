package org.acme.card.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.card.infrastructure.persistence.entity.CardStatementEntity;

import java.util.UUID;

@ApplicationScoped
public class CardStatementPanacheRepository implements PanacheRepositoryBase<CardStatementEntity, UUID> {
}
