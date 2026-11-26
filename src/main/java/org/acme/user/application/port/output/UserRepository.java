package org.acme.user.application.port.output;

import org.acme.user.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID id);

    /** email ya normalizado ({@link User#normalizeEmail(String)}). */
    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSubject(String googleSubject);

    boolean existsByEmail(String email);

    /** Inserta si no tiene id; si no, actualiza. Devuelve el usuario persistido. */
    User save(User user);

    /** Usuarios activos (para las tareas programadas). */
    List<User> findActive();
}
