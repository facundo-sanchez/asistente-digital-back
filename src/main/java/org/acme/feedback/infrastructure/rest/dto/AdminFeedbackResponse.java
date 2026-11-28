package org.acme.feedback.infrastructure.rest.dto;

import org.acme.feedback.application.model.FeedbackEntry;
import org.acme.feedback.domain.model.FeedbackKind;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Un comentario o reporte con quién lo mandó y los datos técnicos (solo administradores). */
@Schema(name = "AdminFeedbackResponse")
public record AdminFeedbackResponse(UUID id, FeedbackKind kind, String message, String page, String details, String userAgent,
                                    Instant createdAt, String userName, String userEmail) {

    public static AdminFeedbackResponse from(FeedbackEntry e) {
        var f = e.feedback();
        return new AdminFeedbackResponse(f.id(), f.kind(), f.message(), f.page(), f.details(), f.userAgent(), f.createdAt(), e.userName(), e.userEmail());
    }
}
