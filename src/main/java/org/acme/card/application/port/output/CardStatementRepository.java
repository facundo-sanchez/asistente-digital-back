package org.acme.card.application.port.output;

import org.acme.card.domain.model.CardStatement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Resúmenes de tarjeta de un usuario. Toda consulta va filtrada por userId. */
public interface CardStatementRepository {

    /** Inserta si no tiene id; si no, actualiza. */
    CardStatement save(CardStatement statement);

    Optional<CardStatement> findByIdForUser(UUID userId, UUID statementId);

    /** Los de la tarjeta, anulados incluidos, por vencimiento (más nuevos primero). */
    List<CardStatement> findForCard(UUID userId, UUID cardId);

    /** El confirmado de esa tarjeta con ese vencimiento (RN-016). */
    Optional<CardStatement> findConfirmedByDueDate(UUID userId, UUID cardId, LocalDate dueDate);

    /** El resumen que generó ese vencimiento (en pesos o en dólares). */
    Optional<CardStatement> findByObligation(UUID userId, UUID obligationId);
}
