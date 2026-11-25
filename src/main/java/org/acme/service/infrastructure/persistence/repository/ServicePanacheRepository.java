package org.acme.service.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.service.infrastructure.persistence.entity.ServiceEntity;

import java.util.UUID;

@ApplicationScoped
public class ServicePanacheRepository implements PanacheRepositoryBase<ServiceEntity, UUID> {
}
