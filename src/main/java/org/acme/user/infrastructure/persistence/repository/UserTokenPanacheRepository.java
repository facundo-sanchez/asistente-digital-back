package org.acme.user.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.user.infrastructure.persistence.entity.UserTokenEntity;

import java.util.UUID;

@ApplicationScoped
public class UserTokenPanacheRepository implements PanacheRepositoryBase<UserTokenEntity, UUID> {
}
