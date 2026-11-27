package org.acme.loan.infrastructure.ai;

/** Instrucciones para leer el comprobante de pago de una cuota de préstamo (esquema loan_receipt/1). */
final class LoanReceiptPrompt {

    private LoanReceiptPrompt() {
    }

    static final String TEXT = """
            Este documento es el comprobante de pago de una cuota de un préstamo en Argentina. Extraé los datos con este
            formato JSON exacto (un solo objeto, null en lo que no figure):
            {
              "lender": "banco o entidad (ej. 'BBVA')",
              "loanNumber": "número de préstamo tal como figura, sin espacios",
              "principal": monto original del préstamo,
              "disbursementDate": "fecha de desembolso yyyy-MM-dd",
              "installmentNumber": número de la cuota pagada (entero),
              "pendingInstallments": cuotas pendientes después de este pago (entero),
              "dueDate": "fecha de vencimiento de la cuota yyyy-MM-dd",
              "paymentDate": "fecha de pago de la cuota yyyy-MM-dd",
              "totalAmount": importe total pagado,
              "currency": "ARS o USD (PESOS = ARS)",
              "capital": amortización de capital,
              "interest": intereses,
              "vat": IVA,
              "stamps": sellados,
              "previousBalance": saldo anterior,
              "newBalance": nuevo saldo
            }
            Los importes con punto decimal y sin separador de miles. No incluyas datos del titular.""";
}
