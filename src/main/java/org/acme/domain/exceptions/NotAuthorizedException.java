package org.acme.domain.exceptions;

public class NotAuthorizedException extends DomainException {
    public NotAuthorizedException() {
        super("No hay un usuario autenticado en la sesión actual");
    }

}