package org.acme.calendar.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.calendar.application.port.output.CalendarFeedRepository;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.shared.application.AppSettings;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

/**
 * Link iCal privado (módulo 19): los vencimientos sin pagar del último mes y los próximos 12, para
 * Google Calendar o el calendario del celular. El token es la clave del link; regenerarlo invalida el anterior.
 */
@ApplicationScoped
public class CalendarUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CalendarFeedRepository feeds;
    private final ObligationRepository obligations;
    private final AppSettings settings;
    private final Clock clock;

    @Inject
    public CalendarUseCase(CalendarFeedRepository feeds, ObligationRepository obligations, AppSettings settings, Clock clock) {
        this.feeds = feeds;
        this.obligations = obligations;
        this.settings = settings;
        this.clock = clock;
    }

    /** El token del usuario; la primera vez lo crea. */
    public String token(UUID userId) {
        return feeds.findToken(userId).orElseGet(() -> regenerate(userId));
    }

    public String regenerate(UUID userId) {
        String token = newToken();
        feeds.save(userId, token);
        return token;
    }

    /** El archivo .ics del link. Un token que no existe responde 404, igual que uno viejo. */
    public String feed(String token) {
        UUID userId = feeds.findUser(token).orElseThrow(() -> new NotFoundException("Calendario no encontrado"));
        LocalDate today = LocalDate.now(clock);
        var open = obligations.findDueBetween(userId, today.minusMonths(1), today.plusMonths(12)).stream()
                .filter(o -> o.effectiveStatus() != EffectiveObligationStatus.PAID)
                .toList();
        return IcsWriter.write(open, settings.frontendUrl().replaceAll("/+$", ""), clock.instant());
    }

    /** 32 bytes al azar: imposible de adivinar. */
    static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
