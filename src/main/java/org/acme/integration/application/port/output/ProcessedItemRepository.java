package org.acme.integration.application.port.output;

import org.acme.integration.domain.model.IntegrationProcessedItem;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ProcessedItemRepository {

    /** Si el mensaje ya se procesó. Los que fallaron no cuentan: se vuelven a intentar. */
    boolean existsMessage(UUID integrationId, String externalMessageId);

    IntegrationProcessedItem save(IntegrationProcessedItem item);

    /** Cuándo llegó el email más viejo que falló (para volver a buscarlo). */
    Optional<Instant> oldestFailed(UUID integrationId);

    /** Lo procesado de la cuenta, lo más nuevo primero. */
    PageResult<IntegrationProcessedItem> findForIntegration(UUID integrationId, PageQuery page);
}
