package org.acme.dashboard.application.usecase;

import org.acme.dashboard.application.model.DashboardSummary;
import org.acme.dashboard.application.model.MonthTotals;
import org.acme.dashboard.application.model.OverdueTotals;
import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.service.application.usecase.InMemoryServiceRepository;
import org.acme.service.domain.model.Service;
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

/** Los números del Inicio. */
class DashboardUseCaseTest {

    private InMemoryObligationRepository obligations;
    private InMemoryServiceRepository services;
    private DashboardUseCase useCase;
    private UUID ana;

    @BeforeEach
    void setUp() {
        obligations = new InMemoryObligationRepository();
        services = new InMemoryServiceRepository();
        // 02:00 UTC del 1/12 = 23:00 del 30/11 en Buenos Aires: el mes del usuario todavía es noviembre
        Clock clock = Clock.fixed(Instant.parse("2026-12-01T02:00:00Z"), ZoneOffset.UTC);
        InMemoryUserRepository users = new InMemoryUserRepository(clock);
        ana = users.save(User.builder().email("ana@mail.com").name("Ana").timezone("America/Argentina/Buenos_Aires").build()).getId();
        useCase = new DashboardUseCase(obligations, services, users, clock);
    }

    @Test
    void elMesEsElDelUsuarioYLasMonedasVanSeparadas() {
        UUID light = obligation(ana, "40000", "ARS", LocalDate.of(2026, 11, 10));
        obligation(ana, "30", "USD", LocalDate.of(2026, 11, 30));
        obligation(ana, null, null, LocalDate.of(2026, 11, 15));
        obligation(ana, "99999", "ARS", LocalDate.of(2026, 12, 1)); // diciembre: afuera
        UUID cancelled = obligation(ana, "5000", "ARS", LocalDate.of(2026, 11, 20));
        obligations.rows.get(cancelled).setStatus(ObligationStatus.CANCELLED);
        obligations.paid.put(light, new BigDecimal("15000"));

        DashboardSummary summary = useCase.summary(ana);

        assertEquals(LocalDate.of(2026, 11, 1), summary.monthStart());
        assertEquals(LocalDate.of(2026, 11, 30), summary.monthEnd());
        assertEquals(List.of(
                new MonthTotals("ARS", new BigDecimal("40000"), new BigDecimal("15000"), new BigDecimal("25000")),
                new MonthTotals("USD", new BigDecimal("30"), BigDecimal.ZERO, new BigDecimal("30"))), summary.month());
        assertEquals(1, summary.withoutAmount());
    }

    @Test
    void cuentaLoVencidoYLosServiciosActivos() {
        UUID overdue = obligation(ana, "1000", "ARS", LocalDate.of(2026, 10, 5));
        obligations.rows.get(overdue).setStatus(ObligationStatus.PENDING);
        services.save(Service.builder().userId(ana).providerId(UUID.randomUUID()).name("Luz").active(true).build());
        services.save(Service.builder().userId(ana).providerId(UUID.randomUUID()).name("Gas viejo").active(false).build());

        // El repositorio en memoria no calcula "vencida" (lo hace la vista SQL): se prueba la suma por moneda
        assertEquals(List.of(new OverdueTotals("ARS", 1, new BigDecimal("1000"))),
                DashboardUseCase.overdueTotals(obligations.findDueBetween(ana, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))));
        assertEquals(1, useCase.summary(ana).activeServices());
        assertEquals(0, useCase.summary(UUID.randomUUID()).activeServices(), "otro usuario no ve nada");
    }

    private UUID obligation(UUID userId, String amount, String currency, LocalDate due) {
        return obligations.save(Obligation.builder().userId(userId).title("x").dueDate(due)
                .amount(amount == null ? null : new BigDecimal(amount)).currency(currency).build()).obligation().getId();
    }
}
