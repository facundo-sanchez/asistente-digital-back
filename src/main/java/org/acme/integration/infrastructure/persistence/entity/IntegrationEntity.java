package org.acme.integration.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.integration.domain.model.IntegrationProvider;
import org.acme.integration.domain.model.IntegrationStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "integrations")
@Getter
@Setter
public class IntegrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private IntegrationProvider provider;

    @Column(name = "external_account_id", nullable = false)
    private String externalAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private IntegrationStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scopes", nullable = false, columnDefinition = "jsonb")
    private List<String> scopes = new ArrayList<>();

    @Column(name = "sync_cursor")
    private String syncCursor;

    @Column(name = "sync_from")
    private LocalDate syncFrom;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void ensureDefaults() {
        if (scopes == null) scopes = new ArrayList<>();
    }
}
