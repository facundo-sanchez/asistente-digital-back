package org.acme.domain.exceptions;

public class NotFoundException extends DomainException {
    public NotFoundException(Long id) {
        super("No hubo resultados con ID: " + id);
    }
    public NotFoundException(String string) {
        super("No hubo resultados con: " + string);
    }
}