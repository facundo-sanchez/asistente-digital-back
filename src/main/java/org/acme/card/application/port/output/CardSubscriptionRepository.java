package org.acme.card.application.port.output;

import org.acme.card.application.model.SubscriptionCharge;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Consumos candidatos a suscripción y los comercios que el usuario dijo que no lo son. */
public interface CardSubscriptionRepository {

    /** Compras sin cuotas de resúmenes confirmados que cerraron desde since; cardId null = todas las tarjetas. */
    List<SubscriptionCharge> charges(UUID userId, UUID cardId, LocalDate since);

    Set<String> dismissed(UUID userId);

    void dismiss(UUID userId, String merchant, Instant now);

    void restore(UUID userId, String merchant);
}
