package org.acme.integration.application.usecase;

import org.acme.user.domain.model.EmailCheckInterval;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EmailCheckIntervalTest {

    private static final ZoneId BA = ZoneId.of("America/Argentina/Buenos_Aires");
    private final ZonedDateTime now = ZonedDateTime.of(2026, 11, 27, 10, 0, 0, 0, BA);

    @Test
    void cadaHoraRevisaALos61MinutosYNoALos30() {
        assertFalse(GmailSyncUseCase.due(EmailCheckInterval.HOURLY, now.minusMinutes(30).toInstant(), now));
        assertTrue(GmailSyncUseCase.due(EmailCheckInterval.HOURLY, now.minusMinutes(61).toInstant(), now));
        assertTrue(GmailSyncUseCase.due(EmailCheckInterval.EVERY_15_MINUTES, now.minusMinutes(15).toInstant(), now));
    }

    @Test
    void unaVezPorDiaDesdeLas8YManualNunca() {
        Instant yesterday = now.minusDays(1).toInstant();
        assertTrue(GmailSyncUseCase.due(EmailCheckInterval.DAILY, yesterday, now));
        assertFalse(GmailSyncUseCase.due(EmailCheckInterval.DAILY, now.withHour(8).toInstant(), now), "ya revisó hoy");
        assertFalse(GmailSyncUseCase.due(EmailCheckInterval.DAILY, yesterday, now.withHour(7)), "antes de las 8");
        assertFalse(GmailSyncUseCase.due(EmailCheckInterval.MANUAL, yesterday, now));
        assertFalse(GmailSyncUseCase.due(EmailCheckInterval.MANUAL, null, now));
    }
}
