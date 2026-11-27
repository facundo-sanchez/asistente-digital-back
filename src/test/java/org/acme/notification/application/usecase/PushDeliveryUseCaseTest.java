package org.acme.notification.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.acme.notification.application.model.PushGoneException;
import org.acme.notification.application.port.output.PushSender;
import org.acme.notification.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PushDeliveryUseCaseTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC);
    private final InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
    private final InMemoryPushSubscriptionRepository subscriptions = new InMemoryPushSubscriptionRepository();
    private final List<String> sent = new ArrayList<>();
    private final UUID ana = UUID.randomUUID();

    /** Manda a todos salvo al endpoint "gone", que responde 410. */
    private final PushSender sender = new PushSender() {
        public boolean available() { return true; }
        public String publicKey() { return "clave"; }
        public void send(PushSubscription s, String payload) {
            if (s.getEndpoint().endsWith("gone")) throw new PushGoneException("410");
            sent.add(s.getEndpoint() + " " + payload);
        }
    };
    private final PushDeliveryUseCase delivery = new PushDeliveryUseCase(notifications, subscriptions, sender, new ObjectMapper(), clock);

    @Test
    void mandaATodosLosDispositivosYBorraLosQueYaNoExisten() {
        subscriptions.save(PushSubscription.builder().userId(ana).endpoint("https://push/celu").p256dh("k").auth("a").build());
        subscriptions.save(PushSubscription.builder().userId(ana).endpoint("https://push/gone").p256dh("k").auth("a").build());
        UUID obligation = UUID.randomUUID();
        notifications.saveIfAbsent(push("Gas casa", obligation));

        assertEquals(1, delivery.send());

        assertEquals(1, sent.size());
        assertTrue(sent.getFirst().contains("\"url\":\"/obligations/" + obligation + "\""));
        assertEquals(1, subscriptions.rows.size(), "la suscripción vencida se borra");
        assertEquals(NotificationStatus.SENT, notifications.rows.getFirst().getStatus());
        assertEquals(0, delivery.send(), "no se manda dos veces");
    }

    @Test
    void sinDispositivosSeCancela() {
        notifications.saveIfAbsent(push("Gas casa", UUID.randomUUID()));

        assertEquals(0, delivery.send());
        assertEquals(NotificationStatus.CANCELLED, notifications.rows.getFirst().getStatus());
    }

    private Notification push(String title, UUID obligationId) {
        return Notification.builder().userId(ana).notificationType(NotificationType.OBLIGATION_DUE_SOON).channel(NotificationChannel.PUSH)
                .idempotencyKey(UUID.randomUUID().toString()).obligationId(obligationId).title(title).message("Vence mañana")
                .scheduledAt(Instant.parse("2026-11-26T11:00:00Z")).status(NotificationStatus.PENDING).build();
    }
}
