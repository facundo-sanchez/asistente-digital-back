package org.acme.card.infrastructure.ai;

/** Instrucciones para que la IA lea un resumen de tarjeta (esquema card_statement/1). Genérico: no depende del banco. */
final class CardStatementPrompt {

    private CardStatementPrompt() {
    }

    static final String TEXT = """
            Este documento es un resumen (liquidación) de tarjeta de crédito de un banco argentino, o la planilla
            de "movimientos del período" que ofrece el banco. Extraé los datos con este formato JSON exacto
            (un solo objeto, null en lo que no figure):
            {
              "bank": "nombre del banco o emisor",
              "accountLastFour": "últimos 4 dígitos del número de cuenta, si figura (ej. 'cuenta 1270394521' -> '4521')",
              "closingDate": "cierre actual yyyy-MM-dd",
              "dueDate": "vencimiento actual yyyy-MM-dd",
              "nextClosingDate": "próximo cierre yyyy-MM-dd",
              "nextDueDate": "próximo vencimiento yyyy-MM-dd",
              "previousBalance": saldo anterior en pesos,
              "previousBalanceUsd": saldo anterior en dólares,
              "totalAmount": saldo actual / total a pagar en pesos,
              "totalAmountUsd": saldo actual / total a pagar en dólares,
              "minimumPayment": pago mínimo,
              "cards": [ { "lastFour": "últimos 4 de la tarjeta (ej. 'Tarjeta Nro ****0002' -> '0002')", "holderName": "titular",
                           "total": total de consumos en pesos de esa tarjeta, "totalUsd": total en dólares } ],
              "transactions": [ {
                  "date": "fecha del movimiento yyyy-MM-dd (en cuotas, la fecha de la compra original)",
                  "description": "texto tal como figura",
                  "merchant": "comercio, sin el prefijo del medio de pago (ej. 'MERPAGO*RAMENS' -> 'RAMENS')",
                  "installmentNumber": cuota actual (ej. 'C.10/12' o '10/12' -> 10),
                  "installmentTotal": total de cuotas (ej. 12),
                  "amount": importe con signo: positivo cargo, negativo pago o devolución,
                  "currency": "ARS" o "USD" según la columna,
                  "type": "PURCHASE | INSTALLMENT | REFUND | FEE | TAX | INTEREST | PAYMENT | ADJUSTMENT",
                  "cardLastFour": "últimos 4 de la tarjeta del movimiento, si se sabe",
                  "reference": "número de cupón o comprobante"
              } ]
            }
            Reglas: incluí TODOS los movimientos (consumos, cuotas, pagos, impuestos, intereses, comisiones), no los totales
            ni subtotales como movimientos. Un movimiento en cuotas es INSTALLMENT. "SU PAGO" es PAYMENT y va negativo.
            Impuestos (ej. sellos) son TAX. Si una fila tiene importe en las dos monedas, generá un movimiento por moneda.
            Fechas tipo '29-Oct-26' o '29/10/2026' -> '2026-10-29'.""";
}
