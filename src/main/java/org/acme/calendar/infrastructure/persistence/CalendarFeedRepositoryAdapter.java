package org.acme.calendar.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.calendar.application.port.output.CalendarFeedRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CalendarFeedRepositoryAdapter implements CalendarFeedRepository {

    @Inject
    EntityManager em;

    @Override
    public Optional<String> findToken(UUID userId) {
        return Optional.ofNullable(em.find(CalendarFeedEntity.class, userId)).map(CalendarFeedEntity::getToken);
    }

    @Override
    public Optional<UUID> findUser(String token) {
        return em.createQuery("select f.userId from CalendarFeedEntity f where f.token = :token", UUID.class)
                .setParameter("token", token).getResultStream().findFirst();
    }

    @Override
    @Transactional
    public void save(UUID userId, String token) {
        CalendarFeedEntity entity = em.find(CalendarFeedEntity.class, userId);
        if (entity == null) {
            entity = new CalendarFeedEntity();
            entity.setUserId(userId);
            entity.setToken(token);
            entity.setCreatedAt(Instant.now());
            em.persist(entity);
        } else {
            entity.setToken(token);
            entity.setCreatedAt(Instant.now());
        }
    }
}
