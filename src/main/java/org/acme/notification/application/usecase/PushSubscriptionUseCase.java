package org.acme.notification.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exceptions.ServiceException;
import org.acme.domain.exceptions.UnavailableException;
import org.acme.notification.application.port.output.PushSender;
import org.acme.notification.application.port.output.PushSubscriptionRepository;
import org.acme.notification.domain.model.PushSubscription;

import java.util.UUID;

/** Activar y desactivar las notificaciones push de un dispositivo. */
@ApplicationScoped
public class PushSubscriptionUseCase {

    static final String UNAVAILABLE = "Las notificaciones push no están disponibles en este momento";

    private final PushSubscriptionRepository subscriptions;
    private final PushSender sender;

    @Inject
    public PushSubscriptionUseCase(PushSubscriptionRepository subscriptions, PushSender sender) {
        this.subscriptions = subscriptions;
        this.sender = sender;
    }

    public String publicKey() {
        if (!sender.available()) {
            throw new UnavailableException(UNAVAILABLE);
        }
        return sender.publicKey();
    }

    public void subscribe(UUID userId, PushSubscription subscription) {
        if (!sender.available()) {
            throw new UnavailableException(UNAVAILABLE);
        }
        if (!subscription.getEndpoint().startsWith("https://")) {
            throw new ServiceException("La suscripción no es válida");
        }
        subscriptions.save(subscription.toBuilder().id(null).userId(userId).build());
    }

    public void unsubscribe(UUID userId, String endpoint) {
        subscriptions.delete(userId, endpoint);
    }
}
