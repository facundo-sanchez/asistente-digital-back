package org.acme.invoice.application.port.output;

import org.acme.invoice.domain.model.Invoice;

import java.math.BigDecimal;
import java.util.UUID;

/** Avisa que una factura vino más cara que las anteriores (lo implementa el módulo de avisos). */
public interface PriceIncreaseAlerts {

    void increased(Invoice invoice, String serviceName, UUID obligationId, BigDecimal percent, BigDecimal average);
}
