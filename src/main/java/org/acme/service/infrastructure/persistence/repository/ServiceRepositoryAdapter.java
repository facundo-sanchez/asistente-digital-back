package org.acme.service.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.service.application.model.ServiceFilter;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.shared.domain.model.ServiceCategory;
import org.acme.service.infrastructure.persistence.entity.ServiceEntity;
import org.acme.service.infrastructure.persistence.mapper.ServicePersistenceMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;
import org.acme.shared.infrastructure.persistence.LikePattern;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adaptador de persistencia del puerto {@link ServiceRepository}. Siempre filtra por userId. */
@ApplicationScoped
public class ServiceRepositoryAdapter implements ServiceRepository {

    @Inject
    ServicePanacheRepository repository;

    @Inject
    ServicePersistenceMapper mapper;

    @Override
    public Service save(Service service) {
        ServiceEntity entity = mapper.toEntity(service);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    public Optional<Service> findByIdForUser(UUID userId, UUID serviceId) {
        return repository.find("id = :id and userId = :userId", Parameters.with("id", serviceId).and("userId", userId))
                .firstResultOptional()
                .map(mapper::toModel);
    }

    @Override
    public Set<UUID> findAutoDebitIds(UUID userId) {
        return repository.getEntityManager()
                .createQuery("select s.id from ServiceEntity s where s.userId = :userId and s.autoDebit = true and s.active = true", UUID.class)
                .setParameter("userId", userId)
                .getResultStream()
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public List<Service> findByProvider(UUID userId, UUID providerId) {
        return repository.list("userId = ?1 and providerId = ?2", userId, providerId).stream().map(mapper::toModel).toList();
    }

    @Override
    public Map<UUID, String> findNames(UUID userId, Collection<UUID> serviceIds) {
        if (serviceIds.isEmpty()) {
            return Map.of();
        }
        return repository.list("userId = ?1 and id in ?2", userId, serviceIds).stream()
                .collect(Collectors.toMap(ServiceEntity::getId, ServiceEntity::getName));
    }

    @Override
    public Map<UUID, ServiceCategory> findCategories(UUID userId, Collection<UUID> serviceIds) {
        if (serviceIds.isEmpty()) {
            return Map.of();
        }
        return repository.list("userId = ?1 and id in ?2", userId, serviceIds).stream()
                .collect(Collectors.toMap(ServiceEntity::getId, ServiceEntity::getCategory));
    }

    @Override
    public PageResult<Service> findForUser(UUID userId, ServiceFilter filter, PageQuery page) {
        StringBuilder where = new StringBuilder("userId = :userId");
        Parameters params = Parameters.with("userId", userId);
        if (filter.text() != null) {
            // El nro. de socio también se busca: es lo que el usuario tiene a mano en la factura
            where.append(" and (lower(name) like :text").append(LikePattern.ESCAPE)
                    .append(" or lower(customerReference) like :text").append(LikePattern.ESCAPE).append(")");
            params.and("text", LikePattern.contains(filter.text()));
        }
        if (filter.category() != null) {
            where.append(" and category = :category");
            params.and("category", filter.category());
        }
        if (filter.active() != null) {
            where.append(" and active = :active");
            params.and("active", filter.active());
        }

        PanacheQuery<ServiceEntity> query = repository.find(where.toString(), Sort.by("name"), params)
                .page(Page.of(page.page(), page.size()));
        return new PageResult<>(query.list().stream().map(mapper::toModel).toList(), page.page(), page.size(), query.count());
    }
}
