package org.acme.notification.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.notification.application.port.output.NotificationRepository;
import org.acme.notification.domain.model.Notification;
import org.acme.notification.domain.model.NotificationChannel;
import org.acme.notification.domain.model.NotificationStatus;
import org.acme.notification.infrastructure.persistence.entity.NotificationEntity;
import org.acme.notification.infrastructure.persistence.mapper.NotificationPersistenceMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NotificationRepositoryAdapter implements NotificationRepository {

    private static final String WEB = "userId = ?1 and channel = ?2 and createdAt >= ?3";

    @Inject
    NotificationPanacheRepository repository;

    @Inject
    NotificationPersistenceMapper mapper;

    @Override
    @Transactional
    public boolean saveIfAbsent(Notification notification) {
        // La clave es única en la base: si dos corridas chocan, la segunda falla y la tarea sigue con el resto
        if (repository.count("idempotencyKey", notification.getIdempotencyKey()) > 0) {
            return false;
        }
        repository.persistAndFlush(mapper.toEntity(notification));
        return true;
    }

    @Override
    @Transactional
    public Notification save(Notification notification) {
        NotificationEntity entity = repository.getEntityManager().merge(mapper.toEntity(notification));
        repository.flush();
        return mapper.toModel(entity);
    }

    @Override
    public Optional<Notification> findByIdForUser(UUID userId, UUID id) {
        return repository.find("id = ?1 and userId = ?2", id, userId).firstResultOptional().map(mapper::toModel);
    }

    @Override
    public PageResult<Notification> findForUser(UUID userId, boolean unreadOnly, Instant since, PageQuery page) {
        PanacheQuery<NotificationEntity> query = repository.find(WEB + (unreadOnly ? " and readAt is null" : ""),
                Sort.descending("createdAt"), userId, NotificationChannel.WEB, since).page(Page.of(page.page(), page.size()));
        return new PageResult<>(query.list().stream().map(mapper::toModel).toList(), page.page(), page.size(), query.count());
    }

    @Override
    public long countUnread(UUID userId, Instant since) {
        return repository.count(WEB + " and readAt is null", userId, NotificationChannel.WEB, since);
    }

    @Override
    @Transactional
    public int markAllRead(UUID userId, Instant now) {
        return repository.update("readAt = ?1 where userId = ?2 and channel = ?3 and readAt is null", now, userId, NotificationChannel.WEB);
    }

    @Override
    public List<Notification> findDuePush(Instant now, int limit) {
        return repository.find("channel = ?1 and status = ?2 and scheduledAt <= ?3", Sort.by("scheduledAt"),
                        NotificationChannel.PUSH, NotificationStatus.PENDING, now)
                .page(Page.ofSize(limit)).list().stream().map(mapper::toModel).toList();
    }

    @Override
    public List<Notification> findDueEmails(Instant now, int limit) {
        return repository.find("channel = ?1 and status = ?2 and scheduledAt <= ?3", Sort.by("scheduledAt"),
                        NotificationChannel.EMAIL, NotificationStatus.PENDING, now)
                .page(Page.ofSize(limit)).list().stream().map(mapper::toModel).toList();
    }
}
