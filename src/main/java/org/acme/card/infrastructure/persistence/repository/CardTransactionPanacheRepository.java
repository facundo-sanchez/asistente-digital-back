package org.acme.card.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.card.infrastructure.persistence.entity.CardTransactionEntity;

import java.util.UUID;

@ApplicationScoped
public class CardTransactionPanacheRepository implements PanacheRepositoryBase<CardTransactionEntity, UUID> {
}
