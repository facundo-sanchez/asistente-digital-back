package org.acme.user.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID id;
    private String email;
    private String name;
    /** Hash bcrypt. Nunca se expone por REST. */
    private String passwordHash;
    private String googleSubject;
    private Instant emailVerifiedAt;
    @Builder.Default
    private String timezone = "America/Argentina/Buenos_Aires";
    @Builder.Default
    private String locale = "es-AR";
    @Builder.Default
    private String defaultCurrency = "ARS";
    @Builder.Default
    private UserRole role = UserRole.USER;
    @Builder.Default
    private boolean active = true;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public void verifyEmail(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
        }
    }
}
