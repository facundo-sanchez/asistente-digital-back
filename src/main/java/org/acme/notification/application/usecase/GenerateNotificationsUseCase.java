package org.acme.notification.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.domain.model.Document;
import org.acme.notification.application.model.NotificationSettings;
import org.acme.notification.application.port.output.NotificationRepository;
import org.acme.notification.domain.model.Notification;
import org.acme.notification.domain.model.NotificationChannel;
import org.acme.notification.domain.model.NotificationStatus;
import org.acme.notification.domain.model.NotificationType;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.user.application.port.output.UserRepository;
import org.acme.user.domain.model.User;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Crea los avisos (CU-007) en cada canal que el usuario tiene activo (RN-009). Cada aviso tiene una clave única: correr
 * la tarea varias veces no duplica nada. Se avisa por umbral (ej. 7, 3, 1 y 0 días): si un día no corrió, al siguiente
 * sale el umbral que corresponde. En la app el aviso queda enviado; por email queda pendiente para el envío diario.
 */
@ApplicationScoped
public class GenerateNotificationsUseCase {

    /** Hora local del usuario a partir de la cual se generan sus avisos del día. */
    static final int HOUR = 9;
    static final List<Integer> DOCUMENT_DAYS = List.of(7, 30);
    /** Cualquier vencido sin pagar se avisa una vez; el año es solo un tope para la consulta. */
    static final int OVERDUE_LOOKBACK = 365;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");

    private final NotificationRepository notifications;
    private final NotificationPreferencesUseCase preferences;
    private final ObligationRepository obligations;
    private final DocumentRepository documents;
    private final UserRepository users;
    private final Clock clock;

    @Inject
    public GenerateNotificationsUseCase(NotificationRepository notifications, NotificationPreferencesUseCase preferences,
                                        ObligationRepository obligations, DocumentRepository documents, UserRepository users, Clock clock) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.obligations = obligations;
        this.documents = documents;
        this.users = users;
        this.clock = clock;
    }

    /** Para cada usuario activo al que ya le llegaron las 9 en su zona. Devuelve cuántos avisos creó. */
    public int generate() {
        // shortcut: recorre todos los usuarios activos; con miles, filtrar en la base por zona horaria
        return users.findActive().stream().mapToInt(this::generateFor).sum();
    }

    int generateFor(User user) {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(zone(user)));
        if (now.getHour() < HOUR) {
            return 0;
        }
        LocalDate today = now.toLocalDate();
        UUID userId = user.getId();
        NotificationSettings settings = preferences.settings(userId);
        int maxDays = NotificationSettings.CHANNELS.stream().flatMap(c -> settings.days(c).stream()).max(Integer::compare).orElse(0);
        int created = 0;

        for (ObligationSummary o : obligations.findDueBetween(userId, today, today.plusDays(maxDays))) {
            if (!unpaid(o)) {
                continue;
            }
            long daysLeft = ChronoUnit.DAYS.between(today, o.obligation().getDueDate());
            for (NotificationChannel channel : NotificationSettings.CHANNELS) {
                List<Integer> days = settings.days(channel);
                if (settings.wants(channel, NotificationType.OBLIGATION_DUE_SOON) && !days.isEmpty() && daysLeft <= days.getLast()) {
                    int threshold = threshold(days, daysLeft);
                    created += add(channel, userId, NotificationType.OBLIGATION_DUE_SOON, "OBLIGATION_DUE:" + o.obligation().getId() + ":" + threshold,
                            o.obligation().getId(), null, o.obligation().getTitle(), dueText(threshold) + amountText(o));
                }
            }
        }
        for (ObligationSummary o : obligations.findDueBetween(userId, today.minusDays(OVERDUE_LOOKBACK), today.minusDays(1))) {
            if (unpaid(o)) {
                created += everywhere(settings, userId, NotificationType.OBLIGATION_OVERDUE, "OBLIGATION_OVERDUE:" + o.obligation().getId(),
                        o.obligation().getId(), null, o.obligation().getTitle(), "Se venció y no está pago" + amountText(o));
            }
        }
        for (Document d : documents.findExpiringBetween(userId, today, today.plusDays(DOCUMENT_DAYS.getLast()))) {
            int threshold = threshold(DOCUMENT_DAYS, ChronoUnit.DAYS.between(today, d.getExpiresOn()));
            created += everywhere(settings, userId, NotificationType.DOCUMENT_EXPIRING, "DOCUMENT_EXPIRING:" + d.getId() + ":" + threshold,
                    null, d.getId(), title(d), "Vence en " + (threshold == 7 ? "menos de una semana" : "menos de un mes"));
        }
        return created;
    }

    /** Aviso al terminar la lectura con IA: la propuesta espera revisión. */
    public void extractionReady(Document document, UUID jobId) {
        everywhere(preferences.settings(document.getUserId()), document.getUserId(), NotificationType.DOCUMENT_DETECTED, "EXTRACTION_READY:" + jobId,
                null, document.getId(), "La IA leyó " + title(document), "Revisá los datos y confirmalos para que queden registrados");
    }

    /** En cada canal que tiene ese aviso activo. */
    private int everywhere(NotificationSettings settings, UUID userId, NotificationType type, String key, UUID obligationId, UUID documentId,
                           String title, String message) {
        return NotificationSettings.CHANNELS.stream().filter(c -> settings.wants(c, type))
                .mapToInt(c -> add(c, userId, type, key, obligationId, documentId, title, message)).sum();
    }

    private int add(NotificationChannel channel, UUID userId, NotificationType type, String key, UUID obligationId, UUID documentId,
                    String title, String message) {
        Instant now = Instant.now(clock);
        boolean web = channel == NotificationChannel.WEB;
        Notification notification = Notification.builder().userId(userId).notificationType(type).channel(channel)
                .idempotencyKey(key + ":" + channel).obligationId(obligationId).documentId(documentId).title(title).message(message)
                .scheduledAt(now).sentAt(web ? now : null).status(web ? NotificationStatus.SENT : NotificationStatus.PENDING).build();
        return notifications.saveIfAbsent(notification) ? 1 : 0;
    }

    /** El umbral más chico que alcanza a daysLeft (ej. faltan 2 días → umbral 3). thresholds va de menor a mayor. */
    static int threshold(List<Integer> thresholds, long daysLeft) {
        return thresholds.stream().filter(t -> t >= daysLeft).findFirst().orElse(thresholds.getLast());
    }

    private static boolean unpaid(ObligationSummary o) {
        return o.effectiveStatus() != EffectiveObligationStatus.PAID && o.effectiveStatus() != EffectiveObligationStatus.CANCELLED
                && (o.balance() == null || o.balance().signum() > 0);
    }

    private static String dueText(int threshold) {
        return switch (threshold) {
            case 0 -> "Vence hoy";
            case 1 -> "Vence mañana";
            default -> "Vence en " + threshold + " días o menos";
        };
    }

    private static String amountText(ObligationSummary o) {
        BigDecimal amount = o.balance() != null ? o.balance() : o.obligation().getAmount();
        if (amount == null || o.obligation().getCurrency() == null) {
            return "";
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(ES_AR);
        format.setCurrency(Currency.getInstance(o.obligation().getCurrency()));
        return " · " + format.format(amount);
    }

    private static String title(Document d) {
        return d.getTitle() != null && !d.getTitle().isBlank() ? d.getTitle() : d.getOriginalFilename();
    }

    private static ZoneId zone(User user) {
        try {
            return user.getTimezone() == null ? DEFAULT_ZONE : ZoneId.of(user.getTimezone());
        } catch (RuntimeException e) {
            return DEFAULT_ZONE;
        }
    }
}
