package org.acme.notification.infrastructure.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.notification.application.port.output.PushSubscriptionRepository;
import org.acme.notification.domain.model.PushSubscription;
import org.acme.notification.infrastructure.persistence.entity.PushSubscriptionEntity;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PushSubscriptionRepositoryAdapter implements PushSubscriptionRepository {

    @Inject
    PushSubscriptionPanacheRepository repository;

    @Override
    @Transactional
    public PushSubscription save(PushSubscription subscription) {
        PushSubscriptionEntity entity = repository.find("endpoint", subscription.getEndpoint()).firstResultOptional().orElseGet(PushSubscriptionEntity::new);
        entity.setUserId(subscription.getUserId());
        entity.setEndpoint(subscription.getEndpoint());
        entity.setP256dh(subscription.getP256dh());
        entity.setAuth(subscription.getAuth());
        entity.setUserAgent(subscription.getUserAgent());
        if (entity.getId() == null) {
            repository.persist(entity);
        }
        repository.flush();
        return toModel(entity);
    }

    @Override
    public List<PushSubscription> findByUser(UUID userId) {
        return repository.list("userId", userId).stream().map(PushSubscriptionRepositoryAdapter::toModel).toList();
    }

    @Override
    public boolean existsForUser(UUID userId) {
        return repository.count("userId", userId) > 0;
    }

    @Override
    @Transactional
    public void delete(UUID userId, String endpoint) {
        repository.delete("userId = ?1 and endpoint = ?2", userId, endpoint);
    }

    @Override
    @Transactional
    public void deleteByEndpoint(String endpoint) {
        repository.delete("endpoint", endpoint);
    }

    private static PushSubscription toModel(PushSubscriptionEntity e) {
        return new PushSubscription(e.getId(), e.getUserId(), e.getEndpoint(), e.getP256dh(), e.getAuth(), e.getUserAgent(), e.getCreatedAt());
    }
}
