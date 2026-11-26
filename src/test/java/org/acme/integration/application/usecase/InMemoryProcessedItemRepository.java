package org.acme.integration.application.usecase;

import org.acme.integration.application.port.output.ProcessedItemRepository;
import org.acme.integration.domain.model.IntegrationProcessedItem;
import org.acme.integration.domain.model.ProcessedItemResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryProcessedItemRepository implements ProcessedItemRepository {

    public final List<IntegrationProcessedItem> rows = new ArrayList<>();

    @Override
    public boolean existsMessage(UUID integrationId, String externalMessageId) {
        return rows.stream().anyMatch(i -> i.getIntegrationId().equals(integrationId) && i.getExternalMessageId().equals(externalMessageId)
                && i.getResult() != ProcessedItemResult.FAILED);
    }

    @Override
    public IntegrationProcessedItem save(IntegrationProcessedItem item) {
        rows.removeIf(i -> i.getIntegrationId().equals(item.getIntegrationId()) && i.getExternalMessageId().equals(item.getExternalMessageId())
                && i.getExternalAttachmentId().equals(item.getExternalAttachmentId()) && i.getResult() == ProcessedItemResult.FAILED);
        rows.add(item.toBuilder().id(UUID.randomUUID()).build());
        return item;
    }
}
