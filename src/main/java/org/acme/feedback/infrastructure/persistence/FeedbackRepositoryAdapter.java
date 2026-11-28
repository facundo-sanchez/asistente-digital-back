package org.acme.feedback.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.feedback.application.port.output.FeedbackRepository;
import org.acme.feedback.application.model.FeedbackEntry;
import org.acme.feedback.domain.model.Feedback;
import org.acme.feedback.domain.model.FeedbackKind;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

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

    @Override
    public PageResult<FeedbackEntry> findAll(FeedbackKind kind, PageQuery page) {
        String where = kind == null ? "" : " where f.kind = :kind";
        var count = em.createQuery("select count(f) from FeedbackEntity f" + where, Long.class);
        var rows = em.createQuery("select f, u.name, u.email from FeedbackEntity f, UserEntity u where u.id = f.userId"
                + (kind == null ? "" : " and f.kind = :kind") + " order by f.createdAt desc", Object[].class);
        if (kind != null) {
            count.setParameter("kind", kind);
            rows.setParameter("kind", kind);
        }
        List<FeedbackEntry> content = rows.setFirstResult(page.page() * page.size()).setMaxResults(page.size()).getResultList().stream()
                .map(r -> new FeedbackEntry(toModel((FeedbackEntity) r[0]), (String) r[1], (String) r[2])).toList();
        return new PageResult<>(content, page.page(), page.size(), count.getSingleResult());
    }

    private static Feedback toModel(FeedbackEntity e) {
        return new Feedback(e.getId(), e.getUserId(), e.getKind(), e.getMessage(), e.getPage(), e.getDetails(), e.getUserAgent(), e.getCreatedAt());
    }
}
