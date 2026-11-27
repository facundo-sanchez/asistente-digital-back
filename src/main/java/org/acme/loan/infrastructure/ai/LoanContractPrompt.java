package org.acme.loan.infrastructure.ai;

/** Instrucciones para leer el comprobante de alta de un préstamo (esquema loan_contract/1). */
final class LoanContractPrompt {

    private LoanContractPrompt() {
    }

    static final String TEXT = """
            Este documento es el comprobante de alta (otorgamiento) de un préstamo en Argentina. Extraé los datos con este
            formato JSON exacto (un solo objeto, null en lo que no figure):
            {
              "lender": "banco o entidad que da el préstamo (ej. 'BBVA')",
              "loanNumber": "número de préstamo tal como figura, sin espacios",
              "loanType": "tipo de préstamo (ej. 'Préstamo personal')",
              "principal": capital o monto del préstamo,
              "currency": "ARS o USD (PESOS = ARS)",
              "rateType": "FIXED si la tasa es fija, VARIABLE si es variable",
              "annualRate": tasa nominal anual (TNA) en porcentaje (ej. 71),
              "disbursementDate": "fecha de alta o desembolso yyyy-MM-dd"
            }
            Los importes con punto decimal y sin separador de miles. No incluyas datos del titular.""";
}
