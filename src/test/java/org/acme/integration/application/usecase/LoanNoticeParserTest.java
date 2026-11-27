package org.acme.integration.application.usecase;

import org.acme.integration.application.model.LoanNotice;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LoanNoticeParserTest {

    @Test
    void leeElAvisoDeBbva() {
        LoanNotice notice = LoanNoticeParser.parse("Titular Ejemplo\n\n La cuota 10 de su préstamo personal 203/2, por ars 584292,14, vence el 07/11/2026.\n\nNunca te vamos a pedir…").orElseThrow();

        assertEquals(10, notice.installmentNumber());
        assertEquals("203/2", notice.reference());
        assertEquals("ARS", notice.currency());
        assertEquals(new BigDecimal("584292.14"), notice.amount());
        assertEquals(LocalDate.of(2026, 11, 7), notice.dueDate());
    }

    @Test
    void aceptaMilesConPuntoYNoLeeOtroTexto() {
        assertEquals(new BigDecimal("1584292.14"), LoanNoticeParser.parse("La cuota 2 de su préstamo hipotecario 9/1, por ars 1.584.292,14, vence el 01/02/2027").orElseThrow().amount());
        assertTrue(LoanNoticeParser.parse("Ya podés descargar tu resumen").isEmpty());
    }
}
