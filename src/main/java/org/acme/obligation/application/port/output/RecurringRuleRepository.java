package org.acme.obligation.application.port.output;

import org.acme.obligation.domain.model.RecurringRule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Reglas de recurrencia de los usuarios. */
public interface RecurringRuleRepository {

    /** Inserta si no tiene id; si no, actualiza. */
    RecurringRule save(RecurringRule rule);

    Optional<RecurringRule> findByIdForUser(UUID userId, UUID ruleId);

    /** Las del usuario, por título. */
    List<RecurringRule> findForUser(UUID userId);

    /** De todos los usuarios: activas con un período pendiente que vence hasta limit (para la tarea diaria). */
    List<RecurringRule> findActiveDueUntil(LocalDate limit);
}
