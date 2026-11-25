package org.acme.domain.exceptions;

/** 401: sin sesión, credenciales inválidas o token inválido. */
public class NotAuthorizedException extends DomainException {
    public NotAuthorizedException() {
        super("No hay un usuario autenticado en la sesión actual");
    }

    public NotAuthorizedException(String message) {
        super(message);
    }
}
