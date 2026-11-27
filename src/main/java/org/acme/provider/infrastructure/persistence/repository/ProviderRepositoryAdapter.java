package org.acme.provider.infrastructure.persistence.repository;

import jakarta.transaction.Transactional;
import org.acme.provider.domain.model.EmailExtractionMode;
import org.acme.provider.infrastructure.persistence.entity.ProviderEmailRuleEntity;
import org.acme.shared.domain.model.DocumentType;

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
import org.acme.shared.infrastructure.persistence.LikePattern;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adaptador de persistencia del puerto {@link ProviderRepository}. */
@ApplicationScoped
public class ProviderRepositoryAdapter implements ProviderRepository {

    /** Catálogo (sin dueño) o del propio usuario. */
    private static final String VISIBLE = "(userId is null or userId = :userId)";

    @Inject
    ProviderPanacheRepository repository;

    @Inject
    ProviderEmailRulePanacheRepository rules;

    @Inject
    ProviderPersistenceMapper mapper;

    @Override
    public PageResult<Provider> findVisible(UUID userId, ProviderFilter filter, PageQuery page) {
        StringBuilder where = new StringBuilder(VISIBLE + " and active = true");
        Parameters params = Parameters.with("userId", userId);
        if (filter.text() != null) {
            where.append(" and lower(name) like :text").append(LikePattern.ESCAPE);
            params.and("text", LikePattern.contains(filter.text()));
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

    @Override
    public boolean existsVisibleWithName(UUID userId, String name) {
        return repository.count(VISIBLE + " and lower(name) = :name",
                Parameters.with("userId", userId).and("name", name.toLowerCase(Locale.ROOT))) > 0;
    }

    @Override
    public boolean existsOwnCode(UUID userId, String code) {
        return repository.count("userId = ?1 and code = ?2", userId, code) > 0;
    }

    @Override
    public Provider save(Provider provider) {
        ProviderEntity entity = mapper.toEntity(provider);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    public Optional<String> findInvoiceSender(UUID providerId) {
        Optional<ProviderEmailRuleEntity> rule = rules.find("providerId", providerId).firstResultOptional();
        if (rule.isEmpty()) {
            return Optional.empty();
        }
        if (rule.get().getSenderAddress() != null) {
            return Optional.of(rule.get().getSenderAddress());
        }
        String[] domains = repository.findById(providerId).getEmailDomains();
        return domains == null || domains.length == 0 ? Optional.empty() : Optional.of(domains[0]);
    }

    @Override
    @Transactional
    public void saveInvoiceSender(UUID providerId, String sender) {
        ProviderEntity provider = repository.findById(providerId);
        if (sender == null) {
            rules.delete("providerId", providerId);
            provider.setEmailDomains(new String[0]);
            return;
        }
        boolean address = sender.contains("@");
        // Una dirección se reconoce exacta; un dominio, por el dominio del proveedor (y sus subdominios)
        provider.setEmailDomains(address ? new String[0] : new String[]{sender});
        ProviderEmailRuleEntity rule = rules.find("providerId", providerId).firstResultOptional().orElseGet(ProviderEmailRuleEntity::new);
        rule.setProviderId(providerId);
        rule.setSenderAddress(address ? sender : null);
        // Adjunto si trae (PDF o imagen); si no, el texto del email: los dos los lee la IA
        rule.setExtractionMode(EmailExtractionMode.EMAIL_BODY);
        rule.setRequiresAttachment(false);
        rule.setDocumentType(DocumentType.INVOICE);
        rule.setPriority(200);
        rule.setActive(true);
        if (rule.getId() == null) {
            rules.persist(rule);
        }
        rules.flush();
    }

    @Override
    public Map<UUID, String> findNames(Collection<UUID> providerIds) {
        if (providerIds.isEmpty()) {
            return Map.of();
        }
        return repository.list("id in ?1", providerIds).stream()
                .collect(Collectors.toMap(ProviderEntity::getId, ProviderEntity::getName));
    }

}
