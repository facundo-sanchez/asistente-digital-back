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
    @Override
    public org.acme.shared.application.model.PageResult<org.acme.feedback.application.model.FeedbackEntry> findAll(org.acme.feedback.domain.model.FeedbackKind kind, org.acme.shared.application.model.PageQuery page) {
        var content = saved.stream().filter(f -> kind == null || f.kind() == kind).map(f -> new org.acme.feedback.application.model.FeedbackEntry(f, "Ana", "ana@mail.com")).toList();
        return new org.acme.shared.application.model.PageResult<>(content, page.page(), page.size(), content.size());
    }
}
