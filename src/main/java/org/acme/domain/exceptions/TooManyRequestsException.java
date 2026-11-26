package org.acme.domain.exceptions;

/** El usuario pasó un límite de uso (HTTP 429). */
public class TooManyRequestsException extends DomainException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
