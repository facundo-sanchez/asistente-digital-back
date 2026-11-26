package org.acme.notification.application.usecase;

import org.acme.notification.application.port.output.NotificationPreferenceRepository;
import org.acme.notification.domain.model.NotificationPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryNotificationPreferenceRepository implements NotificationPreferenceRepository {

    private final List<NotificationPreference> rows = new ArrayList<>();

    @Override
    public List<NotificationPreference> findForUser(UUID userId) {
        return rows.stream().filter(p -> p.getUserId().equals(userId)).map(p -> p.toBuilder().build()).toList();
    }

    @Override
    public void replaceForUser(UUID userId, List<NotificationPreference> preferences) {
        rows.removeIf(p -> p.getUserId().equals(userId));
        preferences.forEach(p -> rows.add(p.toBuilder().id(UUID.randomUUID()).build()));
    }
}
