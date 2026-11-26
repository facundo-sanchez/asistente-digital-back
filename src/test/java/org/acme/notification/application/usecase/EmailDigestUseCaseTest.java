package org.acme.notification.application.usecase;

import org.acme.notification.domain.model.Notification;
import org.acme.notification.domain.model.NotificationChannel;
import org.acme.notification.domain.model.NotificationStatus;
import org.acme.notification.domain.model.NotificationType;
import org.acme.shared.application.port.output.EmailSender;
import org.acme.shared.domain.model.EmailMessage;
import org.acme.user.application.usecase.InMemoryUserRepository;
import org.acme.user.application.usecase.TestAppSettings;
import org.acme.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Un solo email por usuario con todos sus avisos; reintentos si falla. */
class EmailDigestUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-26T11:05:00Z");

    private InMemoryNotificationRepository notifications;
    private InMemoryUserRepository users;
    private final List<EmailMessage> sent = new ArrayList<>();
    private UUID ana;

    @BeforeEach
    void setUp() {
        notifications = new InMemoryNotificationRepository();
        users = new InMemoryUserRepository(Clock.systemUTC());
        ana = users.save(User.builder().email("ana@mail.com").name("Ana").build()).getId();
    }

    private EmailDigestUseCase digest(EmailSender sender, Instant now) {
        return new EmailDigestUseCase(notifications, users, sender, new TestAppSettings(), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void dosAvisosSalenEnUnSoloEmail() {
        UUID light = pending("Luz", "Vence hoy");
        pending("Gas", "Vence mañana");

        assertEquals(1, digest(sent::add, NOW).send());

        assertEquals(1, sent.size());
        assertEquals("Tenés 2 avisos", sent.getFirst().subject());
        assertTrue(sent.getFirst().body().contains("Luz: Vence hoy\n  http://localhost:3000/obligations/" + light));
        assertTrue(notifications.rows.stream().allMatch(n -> n.getStatus() == NotificationStatus.SENT && n.getSentAt() != null));
        assertEquals(0, digest(sent::add, NOW).send(), "no se manda dos veces");
    }

    @Test
    void siFallaSeReintentaYAlQuintoQuedaFallido() {
        pending("Luz", "Vence hoy");
        EmailSender down = message -> {
            throw new IllegalStateException("Connection refused");
        };

        digest(down, NOW).send();
        Notification first = notifications.rows.getFirst();
        assertEquals(1, first.getAttempts());
        assertEquals(NOW.plusSeconds(120), first.getScheduledAt());
        assertEquals(0, digest(down, NOW).send(), "todavía no toca reintentar");

        Instant later = NOW;
        for (int i = 0; i < 4; i++) {
            later = later.plusSeconds(3600);
            digest(down, later).send();
        }
        assertEquals(NotificationStatus.FAILED, notifications.rows.getFirst().getStatus());
        assertEquals("IllegalStateException", notifications.rows.getFirst().getLastError());
    }

    private UUID pending(String title, String message) {
        UUID obligationId = UUID.randomUUID();
        notifications.save(Notification.builder().userId(ana).channel(NotificationChannel.EMAIL).notificationType(NotificationType.OBLIGATION_DUE_SOON)
                .idempotencyKey(title).obligationId(obligationId).title(title).message(message).scheduledAt(NOW).build());
        return obligationId;
    }
}
