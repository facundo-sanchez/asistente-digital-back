package org.acme.integration.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.integration.application.port.output.ProcessedItemRepository;
import org.acme.integration.domain.model.IntegrationProcessedItem;
import org.acme.integration.domain.model.ProcessedItemResult;
import org.acme.integration.infrastructure.persistence.entity.IntegrationProcessedItemEntity;
import org.acme.integration.infrastructure.persistence.mapper.IntegrationPersistenceMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProcessedItemRepositoryAdapter implements ProcessedItemRepository {

    @Inject
    ProcessedItemPanacheRepository repository;

    @Inject
    IntegrationPersistenceMapper mapper;

    @Override
    public boolean existsMessage(UUID integrationId, String externalMessageId) {
        return repository.count("integrationId = ?1 and externalMessageId = ?2 and result <> ?3", integrationId, externalMessageId,
                ProcessedItemResult.FAILED) > 0;
    }

    @Override
    @Transactional
    public IntegrationProcessedItem save(IntegrationProcessedItem item) {
        repository.delete("integrationId = ?1 and externalMessageId = ?2 and externalAttachmentId = ?3 and result = ?4",
                item.getIntegrationId(), item.getExternalMessageId(), item.getExternalAttachmentId(), ProcessedItemResult.FAILED);
        IntegrationProcessedItemEntity entity = mapper.toEntity(item);
        repository.persistAndFlush(entity);
        return mapper.toModel(entity);
    }

    @Override
    public Optional<Instant> oldestFailed(UUID integrationId) {
        return repository.find("integrationId = ?1 and result = ?2 and receivedAt is not null", Sort.by("receivedAt"), integrationId,
                ProcessedItemResult.FAILED).firstResultOptional().map(IntegrationProcessedItemEntity::getReceivedAt);
    }

    @Override
    public PageResult<IntegrationProcessedItem> findForIntegration(UUID integrationId, PageQuery page) {
        PanacheQuery<IntegrationProcessedItemEntity> query = repository.find("integrationId", Sort.descending("receivedAt", "processedAt"), integrationId)
                .page(Page.of(page.page(), page.size()));
        return new PageResult<>(query.list().stream().map(mapper::toModel).toList(), page.page(), page.size(), query.count());
    }
}
