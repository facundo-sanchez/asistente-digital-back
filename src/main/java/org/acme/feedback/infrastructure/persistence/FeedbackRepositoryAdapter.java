package org.acme.feedback.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.feedback.application.port.output.FeedbackRepository;
import org.acme.feedback.domain.model.Feedback;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FeedbackRepositoryAdapter implements FeedbackRepository {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public Feedback save(Feedback feedback) {
        FeedbackEntity entity = new FeedbackEntity();
        entity.setUserId(feedback.userId());
        entity.setKind(feedback.kind());
        entity.setMessage(feedback.message());
        entity.setPage(feedback.page());
        entity.setDetails(feedback.details());
        entity.setUserAgent(feedback.userAgent());
        entity.setCreatedAt(feedback.createdAt());
        em.persist(entity);
        return toModel(entity);
    }

    @Override
    public List<Feedback> findForUser(UUID userId, int limit) {
        return em.createQuery("from FeedbackEntity f where f.userId = :userId order by f.createdAt desc", FeedbackEntity.class)
                .setParameter("userId", userId).setMaxResults(limit).getResultList().stream().map(FeedbackRepositoryAdapter::toModel).toList();
    }

    private static Feedback toModel(FeedbackEntity e) {
        return new Feedback(e.getId(), e.getUserId(), e.getKind(), e.getMessage(), e.getPage(), e.getDetails(), e.getUserAgent(), e.getCreatedAt());
    }
}
