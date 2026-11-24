package org.acme.domain.exceptions;

public class ServiceException extends DomainException {
    public ServiceException(Long id) {
        super("No hubo resultados con ID: " + id);
    }
    public ServiceException(String string) {
        super(string);
    }
}