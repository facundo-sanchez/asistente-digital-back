package org.acme.audit.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Registro de auditoría (RN-015). Sin tokens, contraseñas ni contenido documental. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private UUID id;
    private UUID userId;
    /** Ej. PAYMENT_REGISTERED, INVOICE_CONFIRMED, INTEGRATION_DISCONNECTED. */
    private String action;
    /** Ej. PAYMENT, INVOICE, INTEGRATION. */
    private String entityType;
    private UUID entityId;
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    private String correlationId;
    private String ipAddress;
    private Instant createdAt;
}
