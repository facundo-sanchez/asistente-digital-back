package org.acme.notification.application.usecase;

import org.acme.document.application.usecase.InMemoryDocumentRepository;
import org.acme.document.domain.model.Document;
import org.acme.notification.domain.model.Notification;
import org.acme.notification.domain.model.NotificationChannel;
import org.acme.notification.domain.model.NotificationPreference;
import org.acme.notification.domain.model.NotificationType;
import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.shared.application.model.PageQuery;
import org.acme.user.application.usecase.InMemoryUserRepository;
import org.acme.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Avisos en la app: umbrales, sin duplicados, zona horaria y aislamiento. */
class GenerateNotificationsUseCaseTest {

    private InMemoryNotificationRepository notifications;
    private InMemoryObligationRepository obligations;
    private InMemoryDocumentRepository documents;
    private InMemoryUserRepository users;
    private NotificationPreferencesUseCase preferences;
    private UUID ana;

    @BeforeEach
    void setUp() {
        notifications = new InMemoryNotificationRepository();
        obligations = new InMemoryObligationRepository();
        documents = new InMemoryDocumentRepository();
        users = new InMemoryUserRepository(Clock.systemUTC());
        preferences = new NotificationPreferencesUseCase(new InMemoryNotificationPreferenceRepository());
        ana = users.save(User.builder().email("ana@mail.com").name("Ana").timezone("America/Argentina/Buenos_Aires").build()).getId();
    }

    /** 12:00 UTC = 9:00 en Buenos Aires. */
    private GenerateNotificationsUseCase at(String instant) {
        return new GenerateNotificationsUseCase(notifications, preferences, obligations, documents, users, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    @Test
    void avisaPorUmbralUnaSolaVezAunqueCorraVariasVeces() {
        obligation("Luz", "40000", LocalDate.of(2026, 11, 28)); // faltan 2 días → umbral 3
        UUID paid = obligation("Gas", "100", LocalDate.of(2026, 11, 27));
        obligations.rows.get(paid).setStatus(ObligationStatus.PAID);
        UUID overdue = obligation("VTV", "115000", LocalDate.of(2026, 11, 25));
        documents.save(Document.builder().userId(ana).title("Póliza").originalFilename("p.pdf").expiresOn(LocalDate.of(2026, 12, 20)).build());

        GenerateNotificationsUseCase useCase = at("2026-11-26T12:00:00Z");
        assertEquals(6, useCase.generate(), "en la app y por email");
        assertEquals(0, useCase.generate(), "la segunda corrida no repite");

        assertEquals(List.of("OBLIGATION_DUE:%s:3:WEB".formatted(idOf("Luz")), "OBLIGATION_OVERDUE:%s:WEB".formatted(overdue)),
                notifications.rows.stream().filter(n -> n.getObligationId() != null && n.getChannel() == NotificationChannel.WEB).map(Notification::getIdempotencyKey).toList());
        assertEquals("Vence en 3 días o menos · $ 40.000,00", message(NotificationType.OBLIGATION_DUE_SOON).replace(' ', ' '));

        assertEquals(2, at("2026-11-28T12:00:00Z").generate(), "el día del vencimiento sale el umbral 0");
        assertEquals("Vence hoy · $ 40.000,00", notifications.rows.getLast().getMessage().replace(' ', ' '));
    }

    @Test
    void antesDeLas9DelUsuarioNoAvisa() {
        obligation("Luz", "40000", LocalDate.of(2026, 11, 26));
        assertEquals(0, at("2026-11-26T11:30:00Z").generate()); // 8:30 en Buenos Aires
        assertEquals(2, at("2026-11-26T12:00:00Z").generate()); // 9:00
    }

    @Test
    void laCampanaSoloVeLosDelUsuario() {
        obligation("Luz", "40000", LocalDate.of(2026, 11, 26));
        at("2026-11-26T12:00:00Z").generate();
        NotificationQueryUseCase query = new NotificationQueryUseCase(notifications, Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC));
        UUID id = notifications.rows.getFirst().getId();

        assertEquals(1, query.unreadCount(ana));
        assertEquals(0, query.unreadCount(UUID.randomUUID()));
        assertThrows(RuntimeException.class, () -> query.markRead(UUID.randomUUID(), id));
        query.markRead(ana, id);
        assertEquals(0, query.unreadCount(ana));
        assertEquals(1, query.list(ana, false, new PageQuery(0, 20)).totalElements());
    }

    @Test
    void respetaLasPreferencias() {
        preferences.replace(ana, List.of(
                pref(NotificationChannel.EMAIL, NotificationType.OBLIGATION_DUE_SOON, false, List.of()),
                pref(NotificationChannel.WEB, NotificationType.ALL, true, List.of(7, 1))));
        obligation("Luz", "40000", LocalDate.of(2026, 11, 28)); // faltan 2 días

        assertEquals(1, at("2026-11-26T12:00:00Z").generate());
        Notification only = notifications.rows.getFirst();
        assertEquals(NotificationChannel.WEB, only.getChannel());
        assertTrue(only.getIdempotencyKey().endsWith(":7:WEB"), "con días 7 y 1, faltando 2 sale el de 7");
    }

    @Test
    void lasPreferenciasCompletanLoQueFaltaConLosValoresPorDefecto() {
        List<NotificationPreference> all = preferences.replace(ana, List.of(
                pref(NotificationChannel.PUSH, NotificationType.ALL, true, List.of(1)))).preferences();
        assertEquals(12, all.size(), "WEB y EMAIL: ALL más 5 tipos; PUSH se ignora");
        assertFalse(preferences.settings(ana).wants(NotificationChannel.EMAIL, NotificationType.DOCUMENT_DETECTED));
        assertEquals(List.of(0, 1, 3), preferences.settings(ana).days(NotificationChannel.EMAIL));
    }

    private NotificationPreference pref(NotificationChannel channel, NotificationType type, boolean enabled, List<Integer> days) {
        return NotificationPreference.builder().channel(channel).notificationType(type).enabled(enabled).daysBefore(days).build();
    }

    private UUID obligation(String title, String amount, LocalDate due) {
        return obligations.save(Obligation.builder().userId(ana).title(title).dueDate(due).amount(new BigDecimal(amount)).currency("ARS").build())
                .obligation().getId();
    }

    private UUID idOf(String title) {
        return obligations.rows.values().stream().filter(o -> o.getTitle().equals(title)).findFirst().orElseThrow().getId();
    }

    private String message(NotificationType type) {
        return notifications.rows.stream().filter(n -> n.getNotificationType() == type).findFirst().orElseThrow().getMessage();
    }
}
