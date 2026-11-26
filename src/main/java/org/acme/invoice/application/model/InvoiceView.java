package org.acme.invoice.application.model;

import org.acme.invoice.domain.model.Invoice;
import org.acme.obligation.domain.model.ObligationSummary;

/** Una factura con el nombre de su servicio y el vencimiento que generó (null si no generó). */
public record InvoiceView(Invoice invoice, String serviceName, ObligationSummary obligation) {}
