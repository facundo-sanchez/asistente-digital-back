package org.acme.invoice.application.model;

import org.acme.document.domain.model.ExtractionValidationStatus;

import java.util.List;
import java.util.UUID;

/**
 * Lo que ve el usuario para revisar una factura leída por la IA: la propuesta, el servicio que le
 * corresponde (por número de socio o proveedor) y los avisos (datos que faltan, repetida).
 */
public record InvoiceReview(
        UUID extractionId,
        UUID documentId,
        ExtractionValidationStatus status,
        ExtractedInvoice invoice,
        UUID suggestedServiceId,
        /** Proveedor del catálogo si llegó por email (para registrar el servicio que falta). */
        UUID suggestedProviderId,
        List<String> warnings,
        UUID invoiceId
) {}
