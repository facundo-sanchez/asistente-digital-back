package org.acme.user.application.port.output;

import org.acme.user.domain.model.UserToken;
import org.acme.user.domain.model.UserTokenType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserTokenRepository {

    UserToken save(UserToken token);

    /** Busca por el hash SHA-256 del token (el valor en claro nunca se guarda). */
    Optional<UserToken> findByHash(String tokenHash);

    /** Marca como usados todos los tokens vigentes de ese tipo. Devuelve cuántos invalidó. */
    int invalidateAll(UUID userId, UserTokenType type, Instant when);
}
