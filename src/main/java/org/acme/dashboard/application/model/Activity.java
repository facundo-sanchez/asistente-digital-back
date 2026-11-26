package org.acme.dashboard.application.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Algo que el usuario registró. kind: PAYMENT, INVOICE, STATEMENT o DOCUMENT.
 * linkId es a dónde lleva: el vencimiento del pago, la tarjeta del resumen o la propia factura/documento.
 */
public record Activity(String kind, UUID id, UUID linkId, String title, BigDecimal amount, String currency, Instant at) {
}
