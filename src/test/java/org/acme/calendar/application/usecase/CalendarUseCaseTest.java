package org.acme.calendar.application.usecase;

import org.acme.domain.exceptions.NotFoundException;
import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.user.application.usecase.TestAppSettings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CalendarUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 27);

    private final InMemoryObligationRepository obligations = new InMemoryObligationRepository();
    private final CalendarUseCase calendar = new CalendarUseCase(new InMemoryCalendarFeedRepository(), obligations, new TestAppSettings(),
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    private final UUID ana = UUID.randomUUID();

    @Test
    void elLinkTraeLosVencimientosSinPagarComoEventosDeTodoElDia() {
        UUID gas = obligations.save(obligation("Gas, Camuzzi", TODAY.plusDays(3), ObligationStatus.PENDING)).obligation().getId();
        obligations.save(obligation("Luz pagada", TODAY.plusDays(5), ObligationStatus.PAID));
        obligations.save(obligation("Muy lejos", TODAY.plusMonths(13), ObligationStatus.PENDING));
        obligations.save(obligation("De Beto", TODAY.plusDays(3), ObligationStatus.PENDING).toBuilder().userId(UUID.randomUUID()).build());

        String ics = calendar.feed(calendar.token(ana));

        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20261130\r\n"));
        assertTrue(ics.contains("SUMMARY:Gas\\, Camuzzi · $ 12.345\\,50"), ics);
        assertTrue(ics.contains("URL:http://localhost:3000/obligations/" + gas));
        assertFalse(ics.contains("Luz pagada"));
        assertFalse(ics.contains("Muy lejos"));
        assertFalse(ics.contains("Beto"));
    }

    @Test
    void generarOtroLinkInvalidaElAnterior() {
        String first = calendar.token(ana);
        assertEquals(first, calendar.token(ana), "el mismo link mientras no se regenere");

        String second = calendar.regenerate(ana);

        assertNotEquals(first, second);
        assertThrows(NotFoundException.class, () -> calendar.feed(first));
        assertTrue(calendar.feed(second).contains("END:VCALENDAR"));
    }

    private Obligation obligation(String title, LocalDate due, ObligationStatus status) {
        return Obligation.builder().userId(ana).title(title).amount(new BigDecimal("12345.50")).currency("ARS").dueDate(due).status(status).build();
    }
}
