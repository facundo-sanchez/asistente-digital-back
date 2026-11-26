package org.acme.obligation.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.obligation.application.model.ObligationFilter;
import org.acme.obligation.application.model.ObligationView;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.shared.application.SearchText;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Consulta de obligaciones del usuario, con el nombre del servicio asociado. */
@ApplicationScoped
public class ObligationQueryUseCase {

    private final ObligationRepository obligations;
    private final ServiceRepository services;

    @Inject
    public ObligationQueryUseCase(ObligationRepository obligations, ServiceRepository services) {
        this.obligations = obligations;
        this.services = services;
    }

    public PageResult<ObligationView> list(UUID userId, ObligationFilter filter, PageQuery page) {
        ObligationFilter normalized = new ObligationFilter(SearchText.normalize(filter.text()), filter.statuses(),
                filter.serviceId(), filter.category(), filter.from(), filter.to());
        PageResult<ObligationSummary> result = obligations.findForUser(userId, normalized, page);
        // Un solo viaje a la base para los nombres de los servicios de la página
        Set<UUID> serviceIds = result.content().stream().map(s -> s.obligation().getServiceId())
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> names = services.findNames(userId, serviceIds);
        return result.map(summary -> new ObligationView(summary, serviceName(names, summary)));
    }

    /** Una obligación de otro usuario responde igual que una inexistente. */
    public ObligationView get(UUID userId, UUID obligationId) {
        ObligationSummary summary = obligations.findByIdForUser(userId, obligationId)
                .orElseThrow(() -> new NotFoundException("obligación"));
        UUID serviceId = summary.obligation().getServiceId();
        Map<UUID, String> names = serviceId == null ? Map.of() : services.findNames(userId, Set.of(serviceId));
        return new ObligationView(summary, serviceName(names, summary));
    }

    private static String serviceName(Map<UUID, String> names, ObligationSummary summary) {
        // Los mapas inmutables no aceptan get(null)
        UUID serviceId = summary.obligation().getServiceId();
        return serviceId == null ? null : names.get(serviceId);
    }
}
