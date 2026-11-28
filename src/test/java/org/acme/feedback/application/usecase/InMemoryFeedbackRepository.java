package org.acme.feedback.application.usecase;

import org.acme.feedback.application.port.output.FeedbackRepository;
import org.acme.feedback.domain.model.Feedback;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class InMemoryFeedbackRepository implements FeedbackRepository {

    final List<Feedback> saved = new ArrayList<>();

    @Override
    public Feedback save(Feedback feedback) {
        saved.add(feedback);
        return feedback;
    }

    @Override
    public List<Feedback> findForUser(UUID userId, int limit) {
        return saved.stream().filter(f -> f.userId().equals(userId)).limit(limit).toList();
    }
}
