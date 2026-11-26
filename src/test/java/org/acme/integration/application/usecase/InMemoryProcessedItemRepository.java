package org.acme.integration.application.usecase;

import org.acme.integration.application.port.output.ProcessedItemRepository;
import org.acme.integration.domain.model.IntegrationProcessedItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryProcessedItemRepository implements ProcessedItemRepository {

    public final List<IntegrationProcessedItem> rows = new ArrayList<>();

    @Override
    public boolean existsMessage(UUID integrationId, String externalMessageId) {
        return rows.stream().anyMatch(i -> i.getIntegrationId().equals(integrationId) && i.getExternalMessageId().equals(externalMessageId));
    }

    @Override
    public IntegrationProcessedItem save(IntegrationProcessedItem item) {
        rows.add(item.toBuilder().id(UUID.randomUUID()).build());
        return item;
    }
}
