package org.acme.obligation.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.obligation.infrastructure.persistence.entity.ObligationViewEntity;

import java.util.UUID;

/** Solo lectura: v_obligations. */
@ApplicationScoped
public class ObligationViewPanacheRepository implements PanacheRepositoryBase<ObligationViewEntity, UUID> {
}
