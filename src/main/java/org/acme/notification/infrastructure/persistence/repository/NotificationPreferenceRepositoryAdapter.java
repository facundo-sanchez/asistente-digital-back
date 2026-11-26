package org.acme.notification.infrastructure.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.notification.application.port.output.NotificationPreferenceRepository;
import org.acme.notification.domain.model.NotificationPreference;
import org.acme.notification.infrastructure.persistence.mapper.NotificationPersistenceMapper;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class NotificationPreferenceRepositoryAdapter implements NotificationPreferenceRepository {

    @Inject
    NotificationPreferencePanacheRepository repository;

    @Inject
    NotificationPersistenceMapper mapper;

    @Override
    public List<NotificationPreference> findForUser(UUID userId) {
        return repository.list("userId", userId).stream().map(mapper::toModel).toList();
    }

    @Override
    @Transactional
    public void replaceForUser(UUID userId, List<NotificationPreference> preferences) {
        repository.delete("userId", userId);
        repository.flush();
        repository.persist(preferences.stream().map(mapper::toEntity));
    }
}
