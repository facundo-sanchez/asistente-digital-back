package org.acme.feedback.application.port.output;

import org.acme.feedback.domain.model.Feedback;

import java.util.List;
import java.util.UUID;

public interface FeedbackRepository {

    Feedback save(Feedback feedback);

    /** Lo que mandó el usuario, lo más nuevo primero. */
    List<Feedback> findForUser(UUID userId, int limit);
}
