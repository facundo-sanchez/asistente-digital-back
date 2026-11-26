package org.acme.dashboard.application.model;

import java.time.Instant;
import java.util.UUID;

/** Lo que leyó la IA y espera que el usuario lo revise; schemaVersion dice si es resumen o factura. */
public record PendingReview(UUID id, String schemaVersion, UUID documentId, String documentTitle, Instant createdAt) {
}
