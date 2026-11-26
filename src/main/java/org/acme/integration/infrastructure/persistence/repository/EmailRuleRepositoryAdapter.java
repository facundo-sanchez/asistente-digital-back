package org.acme.integration.infrastructure.persistence.repository;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.integration.application.model.EmailRule;
import org.acme.integration.application.port.output.EmailRuleRepository;
import org.acme.provider.infrastructure.persistence.entity.ProviderEntity;
import org.acme.provider.infrastructure.persistence.repository.ProviderEmailRulePanacheRepository;
import org.acme.provider.infrastructure.persistence.repository.ProviderPanacheRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reglas activas de proveedores del catálogo (no de proveedores propios del usuario) que están activos. */
@ApplicationScoped
public class EmailRuleRepositoryAdapter implements EmailRuleRepository {

    @Inject
    ProviderEmailRulePanacheRepository rules;

    @Inject
    ProviderPanacheRepository providers;

    @Override
    public List<EmailRule> findActive() {
        Map<UUID, ProviderEntity> catalog = providers.list("userId is null and active = true").stream()
                .collect(Collectors.toMap(ProviderEntity::getId, Function.identity()));
        return rules.list("active = true", Sort.by("priority")).stream()
                .filter(r -> catalog.containsKey(r.getProviderId()))
                .map(r -> new EmailRule(r.getId(), r.getProviderId(), r.getSenderAddress(), domains(catalog.get(r.getProviderId())),
                        r.getSubjectRegex(), r.getAttachmentNameRegex(), r.getDocumentType(), r.getExtractionMode(), r.getCustomerReferenceRegex()))
                .toList();
    }

    private static List<String> domains(ProviderEntity provider) {
        return provider.getEmailDomains() == null ? List.of() : Arrays.asList(provider.getEmailDomains());
    }
}
