package org.acme.card.application;

import org.acme.card.application.model.ExtractedStatement;
import org.acme.card.application.model.ExtractedTransaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Controles sobre lo que leyó la IA, con los ejemplos reales de BBVA. */
class StatementChecksTest {

    @Test
    void elPdfDeBbvaCuadraConElSaldoAnterior() {
        assertEquals(List.of(), StatementChecks.warnings(BbvaSamples.pdf()));
    }

    @Test
    void laPlanillaCuadraPorTarjetaYSoloFaltaElVencimiento() {
        List<String> warnings = StatementChecks.warnings(BbvaSamples.excel());

        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("vencimiento"));
    }

    @Test
    void siFaltaUnMovimientoAvisaLaDiferenciaYUnaCuotaImposible() {
        ExtractedStatement pdf = BbvaSamples.pdf();
        List<ExtractedTransaction> transactions = new ArrayList<>(pdf.transactions());
        transactions.remove(2); // se perdió la carga de combustible
        transactions.add(new ExtractedTransaction(null, "X", "X", 13, 12, BigDecimal.ONE, "ARS", "INSTALLMENT", null, null));
        ExtractedStatement broken = new ExtractedStatement(pdf.bank(), pdf.accountLastFour(), pdf.closingDate(), pdf.dueDate(),
                pdf.nextClosingDate(), pdf.nextDueDate(), pdf.previousBalance(), pdf.previousBalanceUsd(), pdf.totalAmount(),
                pdf.totalAmountUsd(), pdf.minimumPayment(), pdf.cards(), transactions);

        List<String> warnings = StatementChecks.warnings(broken);

        assertEquals(2, warnings.size());
        assertTrue(warnings.get(0).contains("diferencia de $ 69.999,07"));
        assertTrue(warnings.get(1).contains("13 de 12") || warnings.get(1).contains("cuota"));
    }
}
