package org.acme.domain.exceptions;

/** 409: el recurso ya existe o entra en conflicto con el estado actual. */
public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(message);
    }
}
