package org.acme.user.infrastructure.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.user.application.port.output.UserRepository;
import org.acme.user.domain.model.User;
import org.acme.user.infrastructure.persistence.entity.UserEntity;
import org.acme.user.infrastructure.persistence.mapper.UserPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de persistencia del puerto {@link UserRepository}. */
@ApplicationScoped
public class UserRepositoryAdapter implements UserRepository {

    @Inject
    UserPanacheRepository repository;

    @Inject
    UserPersistenceMapper mapper;

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findByIdOptional(id).map(mapper::toModel);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.find("email", email).firstResultOptional().map(mapper::toModel);
    }

    @Override
    public Optional<User> findByGoogleSubject(String googleSubject) {
        return repository.find("googleSubject", googleSubject).firstResultOptional().map(mapper::toModel);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.count("email", email) > 0;
    }

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    public List<User> findActive() {
        return repository.list("active", true).stream().map(mapper::toModel).toList();
    }
}
