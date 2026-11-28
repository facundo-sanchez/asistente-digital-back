package org.acme.feedback.infrastructure.rest.dto;

import org.acme.feedback.domain.model.Feedback;
import org.acme.feedback.domain.model.FeedbackKind;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Lo que mandó el usuario (sin los datos técnicos). */
@Schema(name = "FeedbackResponse")
public record FeedbackResponse(UUID id, FeedbackKind kind, String message, String page, Instant createdAt) {

    public static FeedbackResponse from(Feedback f) {
        return new FeedbackResponse(f.id(), f.kind(), f.message(), f.page(), f.createdAt());
    }
}
