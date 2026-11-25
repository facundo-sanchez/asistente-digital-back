package org.acme.integration.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_credentials")
@Getter
@Setter
public class IntegrationCredentialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "integration_id", nullable = false)
    private UUID integrationId;

    @Column(name = "encrypted_access_token", nullable = false)
    private byte[] encryptedAccessToken;

    @Column(name = "encrypted_refresh_token")
    private byte[] encryptedRefreshToken;

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    @Column(name = "key_version", nullable = false, length = 30)
    private String keyVersion;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
