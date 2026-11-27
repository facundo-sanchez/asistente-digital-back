package org.acme.notification.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.notification.application.model.PushGoneException;
import org.acme.notification.application.port.output.NotificationRepository;
import org.acme.notification.application.port.output.PushSender;
import org.acme.notification.application.port.output.PushSubscriptionRepository;
import org.acme.notification.domain.model.Notification;
import org.acme.notification.domain.model.NotificationStatus;
import org.acme.notification.domain.model.PushSubscription;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Manda las notificaciones push pendientes a cada dispositivo del usuario (módulo 15). Una suscripción que ya no
 * existe se borra; si ningún dispositivo la recibió por un error, se reintenta como el email (2, 4, 8, 16 minutos).
 */
@ApplicationScoped
public class PushDeliveryUseCase {

    static final int MAX_ATTEMPTS = 5;
    static final int BATCH = 200;

    private final NotificationRepository notifications;
    private final PushSubscriptionRepository subscriptions;
    private final PushSender sender;
    private final ObjectMapper json;
    private final Clock clock;

    @Inject
    public PushDeliveryUseCase(NotificationRepository notifications, PushSubscriptionRepository subscriptions, PushSender sender,
                               ObjectMapper json, Clock clock) {
        this.notifications = notifications;
        this.subscriptions = subscriptions;
        this.sender = sender;
        this.json = json;
        this.clock = clock;
    }

    /** Devuelve cuántas notificaciones salieron. */
    public int send() {
        if (!sender.available()) {
            return 0;
        }
        Instant now = Instant.now(clock);
        int sent = 0;
        for (Notification n : notifications.findDuePush(now, BATCH)) {
            sent += deliver(n, now) ? 1 : 0;
        }
        return sent;
    }

    private boolean deliver(Notification n, Instant now) {
        List<PushSubscription> devices = subscriptions.findByUser(n.getUserId());
        int delivered = 0;
        String error = null;
        for (PushSubscription device : devices) {
            try {
                sender.send(device, payload(n));
                delivered++;
            } catch (PushGoneException e) {
                subscriptions.deleteByEndpoint(device.getEndpoint());
            } catch (RuntimeException e) {
                error = e.getClass().getSimpleName();
            }
        }
        if (delivered > 0 || error == null) {
            // Salió, o el usuario ya no tiene dispositivos: no hay a quién reintentar
            n.setStatus(delivered > 0 ? NotificationStatus.SENT : NotificationStatus.CANCELLED);
            n.setSentAt(delivered > 0 ? now : null);
        } else {
            n.setAttempts(n.getAttempts() + 1);
            n.setLastError(error);
            if (n.getAttempts() >= MAX_ATTEMPTS) {
                n.setStatus(NotificationStatus.FAILED);
            } else {
                n.setScheduledAt(now.plus(Duration.ofMinutes(1L << n.getAttempts())));
            }
        }
        notifications.save(n);
        return delivered > 0;
    }

    String payload(Notification n) {
        try {
            return json.writeValueAsString(Map.of("title", n.getTitle(), "body", n.getMessage(), "url", link(n)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Ruta dentro de la app (el service worker le agrega el origen y el basePath). */
    static String link(Notification n) {
        if (n.getObligationId() != null) return "/obligations/" + n.getObligationId();
        if (n.getDocumentId() != null) return "/documents/" + n.getDocumentId();
        if (n.getIntegrationId() != null) return "/integrations";
        return "/notifications";
    }
}
