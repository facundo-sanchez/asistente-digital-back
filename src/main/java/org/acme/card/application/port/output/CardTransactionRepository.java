package org.acme.card.application.port.output;

import org.acme.card.domain.model.CardTransaction;

import java.util.List;
import java.util.UUID;

/** Movimientos de los resúmenes de tarjeta. */
public interface CardTransactionRepository {

    void saveAll(List<CardTransaction> transactions);

    /** Los del resumen del usuario, por fecha. */
    List<CardTransaction> findForStatement(UUID userId, UUID statementId);
}
