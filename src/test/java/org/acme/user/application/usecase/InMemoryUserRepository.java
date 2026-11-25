package org.acme.user.application.usecase;

import org.acme.user.application.port.output.UserRepository;
import org.acme.user.domain.model.User;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Repositorio en memoria. Guarda copias, como una base real: un cambio sin save() no se persiste. */
public class InMemoryUserRepository implements UserRepository {

    private final Map<UUID, User> rows = new HashMap<>();
    private final Clock clock;

    public InMemoryUserRepository(Clock clock) {
        this.clock = clock;
    }

    /** Fila tal como está guardada (para asserts). */
    public User stored(UUID id) {
        return rows.get(id);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(rows.get(id)).map(u -> u.toBuilder().build());
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return rows.values().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst()
                .map(u -> u.toBuilder().build());
    }

    @Override
    public Optional<User> findByGoogleSubject(String googleSubject) {
        return rows.values().stream()
                .filter(u -> googleSubject.equals(u.getGoogleSubject()))
                .findFirst()
                .map(u -> u.toBuilder().build());
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public User save(User user) {
        User copy = user.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
            copy.setCreatedAt(clock.instant());
        }
        copy.setUpdatedAt(clock.instant());
        rows.put(copy.getId(), copy);
        return copy.toBuilder().build();
    }
}
