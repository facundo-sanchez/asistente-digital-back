package org.acme.infrastructure.resources.rest.exceptions;

/** Error de validación de un campo del request. */
public record FieldViolation(String field, String message) {}
