package org.acme.loan.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Con el préstamo BBVA real: $ 7.000.000 a 30 cuotas, TNA 71 %. Los valores salen de los comprobantes del banco. */
class FrenchScheduleTest {

    private final List<ScheduleRow> rows = FrenchSchedule.of(new BigDecimal("7000000"), new BigDecimal("71"), 30);

    @Test
    void laCuota10CoincideConElComprobanteDelBanco() {
        ScheduleRow tenth = rows.get(9);
        assertEquals(new BigDecimal("353288.06"), tenth.interest());
        assertEquals(new BigDecimal("150728.02"), tenth.capital());
        assertEquals(new BigDecimal("74190.49"), tenth.vat());
        // El banco cobra además $ 6.085,57 de sellados: 584.292,14
        assertEquals(new BigDecimal("578206.57"), tenth.amount());
    }

    @Test
    void elSaldoDeCapitalDifiereDelBancoEnCentavosYTerminaEnCero() {
        assertEquals(0, rows.get(10).balance().subtract(new BigDecimal("5660691.74")).abs().compareTo(BigDecimal.ONE) < 0 ? 0 : 1);
        assertEquals(0, rows.get(25).balance().subtract(new BigDecimal("1749806.88")).abs().compareTo(BigDecimal.ONE) < 0 ? 0 : 1);
        assertEquals(0, rows.getLast().balance().signum());
        assertTrue(rows.get(11).amount().compareTo(rows.get(10).amount()) < 0, "la cuota baja mes a mes");
    }

    @Test
    void sinDatosNoCalcula() {
        assertTrue(FrenchSchedule.of(null, new BigDecimal("71"), 30).isEmpty());
        assertTrue(FrenchSchedule.of(new BigDecimal("1000"), null, 30).isEmpty());
    }
}
