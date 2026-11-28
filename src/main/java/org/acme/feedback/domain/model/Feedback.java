package org.acme.feedback.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Un comentario o reporte del usuario sobre la app. details: datos técnicos del error (JSON como texto). */
public record Feedback(UUID id, UUID userId, FeedbackKind kind, String message, String page, String details, String userAgent,
                       Instant createdAt) {
}
