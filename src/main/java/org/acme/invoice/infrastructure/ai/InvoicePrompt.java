package org.acme.invoice.infrastructure.ai;

/** Instrucciones para que la IA lea una factura de servicio (esquema invoice/1). Genérico: no depende de la empresa. */
final class InvoicePrompt {

    private InvoicePrompt() {
    }

    static final String TEXT = """
            Este documento es una factura o aviso de pago de un servicio en Argentina (luz, gas, agua, internet,
            teléfono, seguro, impuesto, etc.). Extraé los datos con este formato JSON exacto (un solo objeto, null en lo que no figure):
            {
              "provider": "empresa que emite la factura (ej. 'Camuzzi Gas Pampeana')",
              "customerReference": "número de cliente, socio, suministro o cuenta del titular",
              "invoiceNumber": "número de factura o comprobante (ej. '0001-00001234')",
              "billingPeriod": "período facturado como yyyy-MM (ej. '2026-12'); si abarca dos meses, el último",
              "issueDate": "fecha de emisión yyyy-MM-dd",
              "dueDate": "primer vencimiento yyyy-MM-dd",
              "totalAmount": importe total a pagar al primer vencimiento,
              "currency": "ARS o USD",
              "paymentCode": "código de pago electrónico o código de barras, tal como figura"
            }
            Si hay varios vencimientos, usá el primero y su importe. No sumes otras facturas o saldos anteriores al total.""";
}
