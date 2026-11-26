package org.acme.card.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un movimiento del resumen tal como lo leyó la IA. amount con signo (pagos y devoluciones,
 * negativos); en compras en cuotas, installmentNumber de installmentTotal (ej. 10 de 12).
 */
public record ExtractedTransaction(
        LocalDate date,
        String description,
        String merchant,
        Integer installmentNumber,
        Integer installmentTotal,
        BigDecimal amount,
        String currency,
        String type,
        String cardLastFour,
        String reference
) {}
