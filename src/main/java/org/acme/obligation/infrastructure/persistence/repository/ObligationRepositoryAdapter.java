package org.acme.obligation.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.obligation.application.model.ObligationFilter;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.obligation.infrastructure.persistence.entity.ObligationEntity;
import org.acme.obligation.infrastructure.persistence.entity.ObligationViewEntity;
import org.acme.obligation.infrastructure.persistence.mapper.ObligationPersistenceMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;
import org.acme.shared.infrastructure.persistence.LikePattern;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adaptador de persistencia del puerto {@link ObligationRepository}. Escribe en obligations y lee de v_obligations. */
@ApplicationScoped
public class ObligationRepositoryAdapter implements ObligationRepository {

    @Inject
    ObligationPanacheRepository repository;

    @Inject
    ObligationViewPanacheRepository view;

    @Inject
    ObligationPersistenceMapper mapper;

    @Override
    public ObligationSummary save(Obligation obligation) {
        ObligationEntity entity = mapper.toEntity(obligation);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        // La vista calcula saldo y estado. refresh: si ya se leyó en esta transacción, la caché la tendría vieja
        ObligationViewEntity summary = view.findById(entity.getId());
        view.getEntityManager().refresh(summary);
        return mapper.toSummary(summary);
    }

    @Override
    public Optional<ObligationSummary> findByIdForUser(UUID userId, UUID obligationId) {
        return view.find("id = ?1 and userId = ?2", obligationId, userId)
                .firstResultOptional()
                .map(mapper::toSummary);
    }

    @Override
    public PageResult<ObligationSummary> findForUser(UUID userId, ObligationFilter filter, PageQuery page) {
        StringBuilder where = new StringBuilder("userId = :userId");
        Parameters params = Parameters.with("userId", userId);
        if (filter.text() != null) {
            where.append(" and lower(title) like :text").append(LikePattern.ESCAPE);
            params.and("text", LikePattern.contains(filter.text()));
        }
        if (filter.statuses() != null && !filter.statuses().isEmpty()) {
            where.append(" and effectiveStatus in :statuses");
            params.and("statuses", filter.statuses());
        }
        if (filter.serviceId() != null) {
            where.append(" and serviceId = :serviceId");
            params.and("serviceId", filter.serviceId());
        }
        if (filter.category() != null) {
            where.append(" and serviceId in (select s.id from ServiceEntity s where s.userId = :userId and s.category = :category)");
            params.and("category", filter.category());
        }
        if (filter.from() != null) {
            where.append(" and dueDate >= :from");
            params.and("from", filter.from());
        }
        if (filter.to() != null) {
            where.append(" and dueDate <= :to");
            params.and("to", filter.to());
        }

        PanacheQuery<ObligationViewEntity> query = view.find(where.toString(), Sort.by("dueDate").and("title"), params)
                .page(Page.of(page.page(), page.size()));
        return new PageResult<>(query.list().stream().map(mapper::toSummary).toList(), page.page(), page.size(), query.count());
    }

    @Override
    public boolean existsForRecurrence(UUID recurrenceId, LocalDate dueDate) {
        return repository.count("recurrenceId = ?1 and dueDate = ?2", recurrenceId, dueDate) > 0;
    }

    @Override
    public Map<UUID, ObligationSummary> findByInvoices(UUID userId, Collection<UUID> invoiceIds) {
        if (invoiceIds.isEmpty()) {
            return Map.of();
        }
        return view.list("userId = ?1 and invoiceId in ?2", userId, invoiceIds).stream()
                .collect(Collectors.toMap(ObligationViewEntity::getInvoiceId, mapper::toSummary));
    }
}
