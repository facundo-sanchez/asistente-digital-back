package org.acme.feedback.application.port.output;

import org.acme.feedback.application.model.FeedbackEntry;
import org.acme.feedback.domain.model.Feedback;
import org.acme.feedback.domain.model.FeedbackKind;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.util.List;
import java.util.UUID;

public interface FeedbackRepository {

    Feedback save(Feedback feedback);

    /** Lo que mandó el usuario, lo más nuevo primero. */
    List<Feedback> findForUser(UUID userId, int limit);

    /** Todos los usuarios (administración), lo más nuevo primero; kind null = todos los tipos. */
    PageResult<FeedbackEntry> findAll(FeedbackKind kind, PageQuery page);
}
