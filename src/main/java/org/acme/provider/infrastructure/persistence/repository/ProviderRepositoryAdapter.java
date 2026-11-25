package org.acme.provider.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.provider.application.model.ProviderFilter;
import org.acme.provider.application.port.output.ProviderRepository;
import org.acme.provider.domain.model.Provider;
import org.acme.provider.infrastructure.persistence.entity.ProviderEntity;
import org.acme.provider.infrastructure.persistence.mapper.ProviderPersistenceMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de persistencia del puerto {@link ProviderRepository}. */
@ApplicationScoped
public class ProviderRepositoryAdapter implements ProviderRepository {

    /** Catálogo (sin dueño) o del propio usuario. */
    private static final String VISIBLE = "(userId is null or userId = :userId)";

    @Inject
    ProviderPanacheRepository repository;

    @Inject
    ProviderPersistenceMapper mapper;

    @Override
    public PageResult<Provider> findVisible(UUID userId, ProviderFilter filter, PageQuery page) {
        StringBuilder where = new StringBuilder(VISIBLE + " and active = true");
        Parameters params = Parameters.with("userId", userId);
        if (filter.text() != null) {
            where.append(" and lower(name) like :text escape '\\'");
            params.and("text", "%" + escapeLike(filter.text().toLowerCase(Locale.ROOT)) + "%");
        }
        if (filter.category() != null) {
            where.append(" and category = :category");
            params.and("category", filter.category());
        }

        PanacheQuery<ProviderEntity> query = repository.find(where.toString(), Sort.by("name"), params)
                .page(Page.of(page.page(), page.size()));
        return new PageResult<>(query.list().stream().map(mapper::toModel).toList(), page.page(), page.size(), query.count());
    }

    @Override
    public Optional<Provider> findVisibleById(UUID userId, UUID providerId) {
        return repository.find("id = :id and " + VISIBLE, Parameters.with("id", providerId).and("userId", userId))
                .firstResultOptional()
                .map(mapper::toModel);
    }

    /** El texto del usuario se busca literal: % y _ no actúan como comodines. */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
