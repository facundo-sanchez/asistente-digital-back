package org.acme.service.application.usecase;

import org.acme.service.application.model.ServiceFilter;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.shared.domain.model.ServiceCategory;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Repositorio en memoria con los mismos filtros que el adaptador real. Guarda copias. */
public class InMemoryServiceRepository implements ServiceRepository {

    private final Map<UUID, Service> rows = new LinkedHashMap<>();

    public Service stored(UUID id) {
        return rows.get(id);
    }

    @Override
    public Map<UUID, String> findNames(UUID userId, Collection<UUID> serviceIds) {
        // Inmutable como el del adaptador real (Map.of / toMap + copyOf): get(null) falla igual
        return Map.copyOf(rows.values().stream()
                .filter(s -> s.getUserId().equals(userId) && serviceIds.contains(s.getId()))
                .collect(Collectors.toMap(Service::getId, Service::getName)));
    }

    @Override
    public Map<UUID, ServiceCategory> findCategories(UUID userId, Collection<UUID> serviceIds) {
        return Map.copyOf(rows.values().stream()
                .filter(s -> s.getUserId().equals(userId) && serviceIds.contains(s.getId()) && s.getCategory() != null)
                .collect(Collectors.toMap(Service::getId, Service::getCategory)));
    }

    @Override
    public Service save(Service service) {
        Service copy = service.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
        }
        rows.put(copy.getId(), copy);
        return copy.toBuilder().build();
    }

    @Override
    public Optional<Service> findByIdForUser(UUID userId, UUID serviceId) {
        return Optional.ofNullable(rows.get(serviceId))
                .filter(s -> s.getUserId().equals(userId))
                .map(s -> s.toBuilder().build());
    }

    @Override
    public List<Service> findByProvider(UUID userId, UUID providerId) {
        return rows.values().stream()
                .filter(s -> s.getUserId().equals(userId) && s.getProviderId().equals(providerId))
                .map(s -> s.toBuilder().build())
                .toList();
    }

    @Override
    public PageResult<Service> findForUser(UUID userId, ServiceFilter filter, PageQuery page) {
        List<Service> matches = rows.values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .filter(s -> filter.text() == null || contains(s.getName(), filter.text()) || contains(s.getCustomerReference(), filter.text()))
                .filter(s -> filter.category() == null || s.getCategory() == filter.category())
                .filter(s -> filter.active() == null || s.isActive() == filter.active())
                .sorted(Comparator.comparing(Service::getName))
                .toList();
        int from = Math.min(page.page() * page.size(), matches.size());
        int to = Math.min(from + page.size(), matches.size());
        return new PageResult<>(matches.subList(from, to), page.page(), page.size(), matches.size());
    }

    private static boolean contains(String value, String text) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT));
    }
}
