package org.acme.service.application.model;

/**
 * Comprobante de un servicio (factura, alta, póliza) leído por la IA, para crear el servicio
 * (esquema service_proof/1). Todo puede venir en null; category y billingFrequency usan los nombres de los enums.
 */
public record ExtractedServiceProof(
        String providerName,
        String serviceName,
        String category,
        String customerReference,
        String billingFrequency
) {
    public static final String SCHEMA_VERSION = "service_proof/1";
}
