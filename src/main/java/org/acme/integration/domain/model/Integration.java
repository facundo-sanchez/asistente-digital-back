package org.acme.integration.domain.model;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Conexión con una fuente externa (CU-003, CU-013). Los tokens viven en IntegrationCredential. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Integration {
    private UUID id;
    private UUID userId;
    private IntegrationProvider provider;
    /** Ej. la dirección de la cuenta Gmail. */
    private String externalAccountId;
    @Builder.Default
    private IntegrationStatus status = IntegrationStatus.ACTIVE;
    @Builder.Default
    private List<String> scopes = new ArrayList<>();
    /** Cursor de sincronización (historyId de Gmail). Uso interno. */
    private String syncCursor;
    /** Desde qué fecha importar correos. null = solo nuevos. */
    private LocalDate syncFrom;
    private Instant lastSyncAt;
    /** Detalle técnico del último error. Uso interno. */
    private String lastError;
    private Instant disconnectedAt;
    private Instant createdAt;
    private Instant updatedAt;

    /** Desconectar no borra la información ya importada (RN-011). */
    public void disconnect(Instant now) {
        status = IntegrationStatus.DISCONNECTED;
        disconnectedAt = now;
    }
}
