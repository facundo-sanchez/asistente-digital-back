package org.acme.obligation.domain.model;

import org.acme.domain.exceptions.ServiceException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** Estado de una obligación según lo pagado. */
class ObligationTest {

    private static final Instant NOW = Instant.parse("2026-11-25T12:00:00Z");

    @Test
    void parcialTotalYDeMas() {
        Obligation luz = Obligation.builder().amount(new BigDecimal("40000")).build();

        luz.applyPaidAmount(new BigDecimal("15000"), NOW);
        assertEquals(ObligationStatus.PARTIALLY_PAID, luz.getStatus());
        assertNull(luz.getPaidAt());

        luz.applyPaidAmount(new BigDecimal("40000.00"), NOW);
        assertEquals(ObligationStatus.PAID, luz.getStatus());
        assertEquals(NOW, luz.getPaidAt());

        luz.applyPaidAmount(new BigDecimal("41000"), NOW.plusSeconds(60));
        assertEquals(NOW, luz.getPaidAt(), "ya pagada: conserva la fecha del pago que la completó");
    }

    @Test
    void sinImporteQuedaPagadaConElPrimerPagoYSinPagosVuelveAPendiente() {
        Obligation vtv = Obligation.builder().build();

        vtv.applyPaidAmount(new BigDecimal("1"), NOW);
        assertEquals(ObligationStatus.PAID, vtv.getStatus());

        vtv.applyPaidAmount(BigDecimal.ZERO, NOW);
        assertEquals(ObligationStatus.PENDING, vtv.getStatus());
        assertNull(vtv.getPaidAt());
    }

    @Test
    void unaCanceladaNoCambiaDeEstado() {
        Obligation seguro = Obligation.builder().status(ObligationStatus.CANCELLED).build();

        assertThrows(ServiceException.class, () -> seguro.applyPaidAmount(BigDecimal.TEN, NOW));
    }
}
