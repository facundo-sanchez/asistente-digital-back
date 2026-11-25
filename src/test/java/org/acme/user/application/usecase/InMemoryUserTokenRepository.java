package org.acme.user.application.usecase;

import org.acme.user.application.port.output.UserTokenRepository;
import org.acme.user.domain.model.UserToken;
import org.acme.user.domain.model.UserTokenType;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Repositorio de tokens en memoria (guarda copias). */
public class InMemoryUserTokenRepository implements UserTokenRepository {

    private final Map<UUID, UserToken> rows = new HashMap<>();

    public Collection<UserToken> all() {
        return rows.values();
    }

    public long activeRefreshTokens(UUID userId) {
        return rows.values().stream()
                .filter(t -> t.getUserId().equals(userId)
                        && t.getTokenType() == UserTokenType.REFRESH
                        && t.getUsedAt() == null)
                .count();
    }

    @Override
    public UserToken save(UserToken token) {
        UserToken copy = token.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
        }
        rows.put(copy.getId(), copy);
        return copy.toBuilder().build();
    }

    @Override
    public Optional<UserToken> findByHash(String tokenHash) {
        return rows.values().stream()
                .filter(t -> t.getTokenHash().equals(tokenHash))
                .findFirst()
                .map(t -> t.toBuilder().build());
    }

    @Override
    public int invalidateAll(UUID userId, UserTokenType type, Instant when) {
        int count = 0;
        for (UserToken t : rows.values()) {
            if (t.getUserId().equals(userId) && t.getTokenType() == type && t.getUsedAt() == null) {
                t.setUsedAt(when);
                count++;
            }
        }
        return count;
    }
}
