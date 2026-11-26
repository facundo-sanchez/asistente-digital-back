package org.acme.invoice.application.model;

import org.acme.invoice.domain.model.InvoiceStatus;

import java.time.LocalDate;
import java.util.UUID;

/** Filtros del listado. null = sin filtro. text busca en el número; from y to acotan el vencimiento (inclusive). */
public record InvoiceFilter(String text, UUID serviceId, InvoiceStatus status, LocalDate from, LocalDate to) {}
