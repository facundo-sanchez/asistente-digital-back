package org.acme.domain.exceptions;

/** Funcionalidad no disponible: sin configurar o con un servicio externo caído (HTTP 503). */
public class UnavailableException extends DomainException {

    public UnavailableException(String message) {
        super(message);
    }
}
