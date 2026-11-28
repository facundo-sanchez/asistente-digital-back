package org.acme.service.infrastructure.ai;

/** Instrucciones para leer el comprobante de un servicio (esquema service_proof/1). */
final class ServiceProofPrompt {

    private ServiceProofPrompt() {
    }

    static final String TEXT = """
            Este documento es una factura, un alta o una póliza de un servicio en Argentina (luz, gas, agua, internet,
            teléfono, TV, seguro, alquiler, prepaga, colegio, suscripción, impuesto). Extraé los datos para registrar el
            servicio con este formato JSON exacto (un solo objeto, null en lo que no figure):
            {
              "providerName": "empresa que presta el servicio, sin 'S.A.' ni 'SRL' (ej. 'Camuzzi Gas Pampeana', 'Edenor', 'La Caja Seguros')",
              "serviceName": "nombre corto para reconocer el servicio (ej. 'Gas', 'Luz', 'Seguro del auto', 'Internet')",
              "category": "una de: ELECTRICITY, GAS, WATER, INTERNET, PHONE, TV, INSURANCE, TAX, SUBSCRIPTION, HEALTH, EDUCATION, RENT, OTHER",
              "customerReference": "número de cliente, de cuenta, de suministro, de socio o de póliza, tal como figura",
              "billingFrequency": "una de: MONTHLY, BIMONTHLY, QUARTERLY, SEMIANNUAL, ANNUAL (según el período facturado o la vigencia)"
            }
            No incluyas el nombre, el documento ni la dirección del titular.""";
}
