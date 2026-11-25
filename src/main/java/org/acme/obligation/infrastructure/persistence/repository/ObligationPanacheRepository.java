package org.acme.obligation.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.obligation.infrastructure.persistence.entity.ObligationEntity;

import java.util.UUID;

@ApplicationScoped
public class ObligationPanacheRepository implements PanacheRepositoryBase<ObligationEntity, UUID> {
}
