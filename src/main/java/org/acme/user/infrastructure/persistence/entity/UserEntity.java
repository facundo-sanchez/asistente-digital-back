package org.acme.user.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.user.domain.model.UserRole;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email", nullable = false, columnDefinition = "citext")
    private String email;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "google_subject")
    private String googleSubject;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "timezone", nullable = false, length = 60)
    private String timezone;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    // CHAR(3) en la base: sin esto Hibernate valida la columna como VARCHAR
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "default_currency", nullable = false, columnDefinition = "char(3)")
    private String defaultCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
