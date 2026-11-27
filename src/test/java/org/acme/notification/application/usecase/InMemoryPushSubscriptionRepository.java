package org.acme.notification.application.usecase;

import org.acme.notification.application.port.output.PushSubscriptionRepository;
import org.acme.notification.domain.model.PushSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryPushSubscriptionRepository implements PushSubscriptionRepository {

    final List<PushSubscription> rows = new ArrayList<>();

    @Override
    public PushSubscription save(PushSubscription subscription) {
        rows.removeIf(s -> s.getEndpoint().equals(subscription.getEndpoint()));
        PushSubscription copy = subscription.toBuilder().id(UUID.randomUUID()).build();
        rows.add(copy);
        return copy;
    }

    @Override
    public List<PushSubscription> findByUser(UUID userId) {
        return rows.stream().filter(s -> s.getUserId().equals(userId)).toList();
    }

    @Override
    public boolean existsForUser(UUID userId) {
        return rows.stream().anyMatch(s -> s.getUserId().equals(userId));
    }

    @Override
    public void delete(UUID userId, String endpoint) {
        rows.removeIf(s -> s.getUserId().equals(userId) && s.getEndpoint().equals(endpoint));
    }

    @Override
    public void deleteByEndpoint(String endpoint) {
        rows.removeIf(s -> s.getEndpoint().equals(endpoint));
    }
}
