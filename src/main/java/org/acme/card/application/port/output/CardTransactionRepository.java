package org.acme.card.application.port.output;

import org.acme.card.domain.model.CardTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Movimientos de los resúmenes de tarjeta. */
public interface CardTransactionRepository {

    void saveAll(List<CardTransaction> transactions);

    /** Los del resumen del usuario, por fecha. */
    List<CardTransaction> findForStatement(UUID userId, UUID statementId);

    Optional<CardTransaction> findByIdForUser(UUID userId, UUID transactionId);

    CardTransaction save(CardTransaction transaction);

    /** Todos los consumos del usuario en ese comercio (merchantName normalizado). */
    List<CardTransaction> findByMerchant(UUID userId, String merchantName);

    /** La cuota number de total de una compra en ese comercio y tarjeta (de resúmenes ya confirmados). */
    List<CardTransaction> findInstallment(UUID userId, UUID cardId, String merchantName, int total, int number);
}
