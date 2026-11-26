package org.acme.integration.application.port.output;

import org.acme.integration.domain.model.IntegrationProcessedItem;

import java.util.UUID;

public interface ProcessedItemRepository {

    /** Si el mensaje ya se procesó. Los que fallaron no cuentan: se vuelven a intentar. */
    boolean existsMessage(UUID integrationId, String externalMessageId);

    IntegrationProcessedItem save(IntegrationProcessedItem item);
}
