package org.acme.domain.exceptions;

public class RepositoryException extends DomainException {
    public RepositoryException(Long id) {
        super("No hubo resultados con ID: " + id);
    }
    public RepositoryException(String string) {
        super("No hubo resultados con: " + string);
    }
}