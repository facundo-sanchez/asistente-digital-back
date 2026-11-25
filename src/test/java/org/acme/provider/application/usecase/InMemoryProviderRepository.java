package org.acme.provider.application.usecase;

import org.acme.provider.application.model.ProviderFilter;
import org.acme.provider.application.port.output.ProviderRepository;
import org.acme.provider.domain.model.Provider;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Repositorio en memoria con la misma regla de visibilidad que el adaptador real. */
public class InMemoryProviderRepository implements ProviderRepository {

    private final List<Provider> rows = new ArrayList<>();
    private ProviderFilter lastFilter;

    public Provider add(Provider provider) {
        Provider stored = provider.toBuilder().id(UUID.randomUUID()).build();
        rows.add(stored);
        return stored;
    }

    public ProviderFilter lastFilter() {
        return lastFilter;
    }

    @Override
    public PageResult<Provider> findVisible(UUID userId, ProviderFilter filter, PageQuery page) {
        lastFilter = filter;
        List<Provider> matches = rows.stream()
                .filter(p -> isVisible(p, userId) && p.isActive())
                .filter(p -> filter.text() == null
                        || p.getName().toLowerCase(Locale.ROOT).contains(filter.text().toLowerCase(Locale.ROOT)))
                .filter(p -> filter.category() == null || p.getCategory() == filter.category())
                .sorted(Comparator.comparing(Provider::getName))
                .toList();
        int from = Math.min(page.page() * page.size(), matches.size());
        int to = Math.min(from + page.size(), matches.size());
        return new PageResult<>(matches.subList(from, to), page.page(), page.size(), matches.size());
    }

    @Override
    public Optional<Provider> findVisibleById(UUID userId, UUID providerId) {
        return rows.stream().filter(p -> p.getId().equals(providerId) && isVisible(p, userId)).findFirst();
    }

    @Override
    public boolean existsVisibleWithName(UUID userId, String name) {
        return rows.stream().anyMatch(p -> isVisible(p, userId) && p.getName().equalsIgnoreCase(name));
    }

    @Override
    public boolean existsOwnCode(UUID userId, String code) {
        return rows.stream().anyMatch(p -> userId.equals(p.getUserId()) && code.equals(p.getCode()));
    }

    @Override
    public Provider save(Provider provider) {
        return add(provider);
    }

    @Override
    public Map<UUID, String> findNames(Collection<UUID> providerIds) {
        return rows.stream()
                .filter(p -> providerIds.contains(p.getId()))
                .collect(Collectors.toMap(Provider::getId, Provider::getName));
    }

    private static boolean isVisible(Provider provider, UUID userId) {
        return provider.getUserId() == null || Objects.equals(provider.getUserId(), userId);
    }
}
