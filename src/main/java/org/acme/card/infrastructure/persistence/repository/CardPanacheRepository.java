package org.acme.card.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.card.infrastructure.persistence.entity.CardEntity;

import java.util.UUID;

@ApplicationScoped
public class CardPanacheRepository implements PanacheRepositoryBase<CardEntity, UUID> {
}
