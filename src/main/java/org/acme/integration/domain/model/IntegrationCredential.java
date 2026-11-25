package org.acme.integration.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Tokens OAuth cifrados por la aplicación. Nunca se exponen por REST. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationCredential {
    private UUID id;
    private UUID integrationId;
    private byte[] encryptedAccessToken;
    private byte[] encryptedRefreshToken;
    private Instant accessTokenExpiresAt;
    /** Versión de la clave de cifrado, para rotación. */
    private String keyVersion;
    private Instant updatedAt;
}
