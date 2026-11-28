package org.acme.feedback.application.model;

import org.acme.feedback.domain.model.Feedback;

/** Un comentario o reporte con quién lo mandó, para la pantalla de administración. */
public record FeedbackEntry(Feedback feedback, String userName, String userEmail) {
}
