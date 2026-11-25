package org.acme.user.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Token de un solo uso (verificación de email, reseteo, refresh). Se guarda solo el hash. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserToken {
    private UUID id;
    private UUID userId;
    private UserTokenType tokenType;
    private String tokenHash;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant createdAt;

    public boolean isUsable(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
