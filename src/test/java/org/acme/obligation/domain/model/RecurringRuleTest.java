package org.acme.obligation.domain.model;

import org.acme.shared.domain.model.BillingFrequency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Fechas de los períodos de una regla y el vencimiento que genera. */
class RecurringRuleTest {

    @Test
    void mensualConservaElDiaYEl31CaeEnElUltimoDiaDeLosMesesCortos() {
        RecurringRule rule = rule(BillingFrequency.MONTHLY, 31);

        assertEquals(LocalDate.of(2027, 2, 28), rule.following(LocalDate.of(2027, 1, 31)));
        assertEquals(LocalDate.of(2027, 3, 31), rule.following(LocalDate.of(2027, 2, 28)), "vuelve al 31 después de febrero");
        assertEquals(LocalDate.of(2028, 2, 29), rule.following(LocalDate.of(2028, 1, 31)), "año bisiesto");
    }

    @Test
    void bimestralTrimestralSemestralAnualYSemanal() {
        LocalDate dec10 = LocalDate.of(2026, 12, 10);
        assertEquals(LocalDate.of(2027, 2, 10), rule(BillingFrequency.BIMONTHLY, 10).following(dec10));
        assertEquals(LocalDate.of(2027, 3, 10), rule(BillingFrequency.QUARTERLY, 10).following(dec10));
        assertEquals(LocalDate.of(2027, 6, 10), rule(BillingFrequency.SEMIANNUAL, 10).following(dec10));
        assertEquals(LocalDate.of(2027, 12, 10), rule(BillingFrequency.ANNUAL, 10).following(dec10));
        assertEquals(LocalDate.of(2026, 12, 17), rule(BillingFrequency.WEEKLY, null).following(dec10));
    }

    @Test
    void generaElVencimientoDelPeriodoYAvanza() {
        RecurringRule rule = rule(BillingFrequency.MONTHLY, 10);

        Obligation december = rule.nextObligation();
        rule.advance();

        assertEquals("Alquiler depto · diciembre 2026", december.getTitle());
        assertEquals(LocalDate.of(2026, 12, 10), december.getDueDate());
        assertEquals(ObligationType.RENT, december.getObligationType());
        assertEquals(new BigDecimal("350000"), december.getAmount());
        assertEquals(rule.getId(), december.getRecurrenceId());
        assertFalse(december.isEstimated());
        assertEquals(LocalDate.of(2027, 1, 10), rule.getNextExpectedDate());
    }

    private static RecurringRule rule(BillingFrequency frequency, Integer day) {
        return RecurringRule.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .title("Alquiler depto").obligationType(ObligationType.RENT)
                .frequency(frequency).dayOfMonth(day)
                .nextExpectedDate(LocalDate.of(2026, 12, 10))
                .estimatedAmount(new BigDecimal("350000")).currency("ARS")
                .build();
    }
}
