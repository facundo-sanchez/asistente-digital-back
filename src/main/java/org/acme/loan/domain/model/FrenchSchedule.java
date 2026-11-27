package org.acme.loan.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Cronograma de un préstamo de tasa fija por sistema francés: capital más interés constante; el interés de cada mes
 * es saldo × TNA / 12 y el IVA, el 21 % del interés. Los sellados no se calculan (no siguen una regla fija): el importe
 * queda unos pesos por debajo del real hasta que llega el comprobante o el aviso del banco.
 */
public final class FrenchSchedule {

    static final BigDecimal VAT = new BigDecimal("0.21");
    private static final MathContext MC = MathContext.DECIMAL64;

    private FrenchSchedule() {
    }

    /** Vacío si falta algún dato para calcularlo. */
    public static List<ScheduleRow> of(BigDecimal principal, BigDecimal annualRate, int count) {
        if (principal == null || principal.signum() <= 0 || annualRate == null || annualRate.signum() <= 0 || count < 1) {
            return List.of();
        }
        BigDecimal rate = annualRate.divide(BigDecimal.valueOf(1200), MC);
        BigDecimal factor = BigDecimal.ONE.add(rate).pow(count, MC);
        // cuota pura = P × i / (1 − (1 + i)^−n)
        BigDecimal payment = principal.multiply(rate, MC).divide(BigDecimal.ONE.subtract(BigDecimal.ONE.divide(factor, MC)), MC);
        List<ScheduleRow> rows = new ArrayList<>(count);
        BigDecimal balance = principal;
        for (int n = 1; n <= count; n++) {
            BigDecimal interest = balance.multiply(rate, MC);
            BigDecimal capital = n == count ? balance : payment.subtract(interest, MC);
            balance = balance.subtract(capital, MC);
            BigDecimal vat = interest.multiply(VAT, MC);
            // Como el banco: el importe es la suma de las partes ya redondeadas
            rows.add(new ScheduleRow(n, money(interest), money(capital), money(vat), money(capital).add(money(interest)).add(money(vat)),
                    money(balance.max(BigDecimal.ZERO))));
        }
        return rows;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
