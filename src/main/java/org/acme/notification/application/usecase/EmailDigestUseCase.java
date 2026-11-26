package org.acme.notification.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.notification.application.port.output.NotificationRepository;
import org.acme.notification.domain.model.Notification;
import org.acme.notification.domain.model.NotificationStatus;
import org.acme.shared.application.AppSettings;
import org.acme.shared.application.port.output.EmailSender;
import org.acme.shared.domain.model.EmailMessage;
import org.acme.user.application.port.output.UserRepository;
import org.acme.user.domain.model.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manda por email los avisos pendientes, uno solo por usuario con todo lo que tiene (todos se generan en la misma
 * corrida de las 9, así que sale uno por día). Si el SMTP falla se reintenta: 2, 4, 8, 16 minutos y después FAILED.
 */
@ApplicationScoped
public class EmailDigestUseCase {

    static final int MAX_ATTEMPTS = 5;
    static final int BATCH = 200;

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final EmailSender email;
    private final AppSettings settings;
    private final Clock clock;

    @Inject
    public EmailDigestUseCase(NotificationRepository notifications, UserRepository users, EmailSender email, AppSettings settings, Clock clock) {
        this.notifications = notifications;
        this.users = users;
        this.email = email;
        this.settings = settings;
        this.clock = clock;
    }

    /** Devuelve cuántos emails salieron. */
    public int send() {
        Instant now = Instant.now(clock);
        Map<UUID, List<Notification>> byUser = notifications.findDueEmails(now, BATCH).stream()
                .collect(Collectors.groupingBy(Notification::getUserId, LinkedHashMap::new, Collectors.toList()));
        int sent = 0;
        for (Map.Entry<UUID, List<Notification>> entry : byUser.entrySet()) {
            sent += sendTo(entry.getKey(), entry.getValue(), now) ? 1 : 0;
        }
        return sent;
    }

    private boolean sendTo(UUID userId, List<Notification> pending, Instant now) {
        Optional<User> user = users.findById(userId).filter(User::isActive);
        if (user.isEmpty()) {
            pending.forEach(n -> close(n, NotificationStatus.CANCELLED, null, null));
            return false;
        }
        try {
            email.send(message(user.get(), pending));
            pending.forEach(n -> close(n, NotificationStatus.SENT, now, null));
            return true;
        } catch (RuntimeException e) {
            // Sin el contenido: solo el tipo de error
            String error = e.getClass().getSimpleName();
            for (Notification n : pending) {
                n.setAttempts(n.getAttempts() + 1);
                if (n.getAttempts() >= MAX_ATTEMPTS) {
                    close(n, NotificationStatus.FAILED, null, error);
                } else {
                    n.setScheduledAt(now.plus(Duration.ofMinutes(1L << n.getAttempts())));
                    n.setLastError(error);
                    notifications.save(n);
                }
            }
            return false;
        }
    }

    private void close(Notification n, NotificationStatus status, Instant sentAt, String error) {
        n.setStatus(status);
        n.setSentAt(sentAt);
        if (error != null) {
            n.setLastError(error);
        }
        notifications.save(n);
    }

    EmailMessage message(User user, List<Notification> pending) {
        String app = settings.frontendUrl().replaceAll("/+$", "");
        String subject = pending.size() == 1 ? pending.getFirst().getTitle() + ": " + pending.getFirst().getMessage()
                : "Tenés " + pending.size() + " avisos";
        StringBuilder body = new StringBuilder("Hola " + user.getName() + ",\n\n");
        for (Notification n : pending) {
            body.append("- ").append(n.getTitle()).append(": ").append(n.getMessage()).append("\n  ").append(app).append(link(n)).append("\n");
        }
        body.append("\nPodés elegir qué avisos recibir por email en ").append(app).append("/profile.\n");
        return new EmailMessage(user.getEmail(), subject, body.toString());
    }

    private static String link(Notification n) {
        if (n.getObligationId() != null) return "/obligations/" + n.getObligationId();
        if (n.getDocumentId() != null) return "/documents/" + n.getDocumentId();
        return "/notifications";
    }
}
