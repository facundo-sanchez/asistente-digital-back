package org.acme.obligation.application.usecase;

import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationType;
import org.acme.obligation.domain.model.RecurringRule;
import org.acme.service.application.usecase.InMemoryServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.shared.domain.model.BillingFrequency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Vencimientos que se repiten: alta, generación diaria sin duplicados, edición y baja. */
class RecurringRuleUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();
    /** "Hoy" en los tests: 25/11/2026. */
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);

    private InMemoryObligationRepository obligations;
    private InMemoryRecurringRuleRepository rules;
    private RecurringObligationGenerator generator;
    private ManageRecurringRuleUseCase useCase;
    private UUID betoGas;

    @BeforeEach
    void setUp() {
        obligations = new InMemoryObligationRepository();
        rules = new InMemoryRecurringRuleRepository();
        InMemoryServiceRepository services = new InMemoryServiceRepository();
        betoGas = services.save(Service.builder().userId(BETO).providerId(UUID.randomUUID()).name("Gas Beto").build()).getId();
        generator = new RecurringObligationGenerator(obligations, rules);
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        useCase = new ManageRecurringRuleUseCase(rules, generator, new ObligationDraftValidator(services), clock);
    }

    @Test
    void alCrearGeneraElPrimerVencimientoYDejaElSiguientePendiente() {
        RecurringRule rule = useCase.create(ANA, rent(LocalDate.of(2026, 12, 10)));

        assertEquals(List.of(LocalDate.of(2026, 12, 10)), dueDates());
        assertEquals(10, rule.getDayOfMonth());
        assertEquals(LocalDate.of(2027, 1, 10), rule.getNextExpectedDate(), "enero está a más de 20 días: todavía no");
        Obligation december = obligations.rows.values().iterator().next();
        assertEquals("Alquiler depto · diciembre 2026", december.getTitle());
        assertEquals(ANA, december.getUserId());
        assertEquals(rule.getId(), december.getRecurrenceId());
    }

    @Test
    void laTareaDiariaCreaElPeriodoSiguienteSinDuplicarYSePoneAlDia() {
        RecurringRule rule = useCase.create(ANA, rent(LocalDate.of(2026, 12, 10)));

        assertEquals(1, generator.generate(rules.rows.get(rule.getId()), LocalDate.of(2026, 12, 25)), "enero entra en los 20 días");
        assertEquals(0, generator.generate(rules.rows.get(rule.getId()), LocalDate.of(2026, 12, 25)), "otra corrida el mismo día no duplica");
        assertEquals(3, generator.generate(rules.rows.get(rule.getId()), LocalDate.of(2027, 4, 1)), "servidor apagado: crea febrero, marzo y abril");
        assertEquals(5, dueDates().size());
    }

    @Test
    void unaReglaDesactivadaNoGeneraYAlReactivarSePoneAlDia() {
        RecurringRule rule = useCase.create(ANA, rent(LocalDate.of(2026, 11, 30)));
        assertEquals(List.of(LocalDate.of(2026, 11, 30)), dueDates());

        useCase.changeStatus(ANA, rule.getId(), false);
        assertEquals(0, generator.generate(rules.rows.get(rule.getId()), LocalDate.of(2027, 1, 15)));

        useCase.changeStatus(ANA, rule.getId(), true);
        assertEquals(1, dueDates().size(), "reactivar con el reloj del 25/11: el 30/12 todavía está a más de 20 días");
    }

    @Test
    void editarCambiaLosProximosPeriodosPeroNoLosYaCreados() {
        RecurringRule rule = useCase.create(ANA, rent(LocalDate.of(2026, 12, 10)));

        useCase.update(ANA, rule.getId(), rent(LocalDate.of(2027, 1, 5)).toBuilder().estimatedAmount(new BigDecimal("420000")).build());
        generator.generate(rules.rows.get(rule.getId()), LocalDate.of(2026, 12, 20));

        List<Obligation> created = obligations.rows.values().stream().sorted(Comparator.comparing(Obligation::getDueDate)).toList();
        assertEquals(new BigDecimal("350000"), created.get(0).getAmount());
        assertEquals(LocalDate.of(2027, 1, 5), created.get(1).getDueDate());
        assertEquals(new BigDecimal("420000"), created.get(1).getAmount());
    }

    @Test
    void validaPeriodicidadServicioYDuenio() {
        assertThrows(ServiceException.class, () -> useCase.create(ANA, rent(TODAY).toBuilder().frequency(BillingFrequency.NONE).build()));
        assertThrows(ServiceException.class, () -> useCase.create(ANA, rent(TODAY).toBuilder().serviceId(betoGas).build()));
        assertThrows(ServiceException.class, () -> useCase.create(ANA, rent(TODAY).toBuilder().currency(null).build()));
        assertTrue(obligations.rows.isEmpty());

        RecurringRule rule = useCase.create(ANA, rent(TODAY));
        assertThrows(NotFoundException.class, () -> useCase.get(BETO, rule.getId()));
        assertThrows(NotFoundException.class, () -> useCase.changeStatus(BETO, rule.getId(), false));
        assertTrue(useCase.list(BETO).isEmpty());
    }

    private List<LocalDate> dueDates() {
        return obligations.rows.values().stream().map(Obligation::getDueDate).sorted().toList();
    }

    private static RecurringRule rent(LocalDate first) {
        return RecurringRule.builder()
                .title("  Alquiler depto ").obligationType(ObligationType.RENT)
                .frequency(BillingFrequency.MONTHLY).nextExpectedDate(first)
                .estimatedAmount(new BigDecimal("350000")).currency("ARS")
                .build();
    }
}
