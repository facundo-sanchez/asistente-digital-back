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

    /**
     * Marca el token como usado solo si nadie lo usó antes (update condicional, atómico en la base).
     * Devuelve false si otro pedido lo usó primero.
     */
    boolean markUsed(UUID tokenId, Instant when);

    /** Revoca todos los tokens vigentes de ese tipo ({@link UserToken#revoke}). Devuelve cuántos revocó. */
    int invalidateAll(UUID userId, UserTokenType type, Instant when);
}
