package org.acme.user.infrastructure.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.user.application.port.output.UserTokenRepository;
import org.acme.user.domain.model.UserToken;
import org.acme.user.domain.model.UserTokenType;
import org.acme.user.infrastructure.persistence.entity.UserTokenEntity;
import org.acme.user.infrastructure.persistence.mapper.UserPersistenceMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de persistencia del puerto {@link UserTokenRepository}. */
@ApplicationScoped
public class UserTokenRepositoryAdapter implements UserTokenRepository {

    @Inject
    UserTokenPanacheRepository repository;

    @Inject
    UserPersistenceMapper mapper;

    @Override
    public UserToken save(UserToken token) {
        UserTokenEntity entity = mapper.toEntity(token);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    public Optional<UserToken> findByHash(String tokenHash) {
        return repository.find("tokenHash", tokenHash).firstResultOptional().map(mapper::toModel);
    }

    @Override
    public boolean markUsed(UUID tokenId, Instant when) {
        return repository.update("usedAt = ?1 where id = ?2 and usedAt is null", when, tokenId) == 1;
    }

    @Override
    public int invalidateAll(UUID userId, UserTokenType type, Instant when) {
        return repository.update("usedAt = ?1, expiresAt = ?1 where userId = ?2 and tokenType = ?3 and usedAt is null",
                when, userId, type);
    }
}
