package org.acme.provider.domain.model;

/**
 * De dónde se extraen los datos de un email de proveedor.
 * ATTACHMENT = PDF adjunto (OCR / IA). EMAIL_BODY = cuerpo del email con regex
 * (ej. BBVA: el resumen de tarjeta no viene adjunto, los totales están en el cuerpo).
 */
public enum EmailExtractionMode {
    ATTACHMENT, EMAIL_BODY,
    /** Aviso de cuota de préstamo en el cuerpo (ej. BBVA): actualiza la cuota, no importa documento (módulo 13.5). */
    LOAN_NOTICE
}
