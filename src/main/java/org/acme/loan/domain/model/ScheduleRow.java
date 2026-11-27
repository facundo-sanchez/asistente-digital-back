package org.acme.loan.domain.model;

import java.math.BigDecimal;

/** Una cuota calculada: interés, capital, IVA sobre el interés, importe (sin sellados) y saldo de capital después de pagarla. */
public record ScheduleRow(int number, BigDecimal interest, BigDecimal capital, BigDecimal vat, BigDecimal amount, BigDecimal balance) {}
