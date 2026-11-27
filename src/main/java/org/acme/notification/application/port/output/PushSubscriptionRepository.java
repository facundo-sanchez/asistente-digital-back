package org.acme.notification.application.port.output;

import org.acme.notification.domain.model.PushSubscription;

import java.util.List;
import java.util.UUID;

public interface PushSubscriptionRepository {

    /** Crea o, si el endpoint ya existe, la pasa a este usuario con las claves nuevas. */
    PushSubscription save(PushSubscription subscription);

    List<PushSubscription> findByUser(UUID userId);

    boolean existsForUser(UUID userId);

    /** Solo si es del usuario. */
    void delete(UUID userId, String endpoint);

    /** El servicio de push dijo que ya no existe (404/410). */
    void deleteByEndpoint(String endpoint);
}
