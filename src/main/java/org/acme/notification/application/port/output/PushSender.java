package org.acme.notification.application.port.output;

import org.acme.notification.domain.model.PushSubscription;

/** Manda una notificación push cifrada (Web Push). */
public interface PushSender {

    boolean available();

    String publicKey();

    /** payload: JSON con title, body y url. Lanza PushGoneException si la suscripción ya no existe. */
    void send(PushSubscription subscription, String payload);
}
