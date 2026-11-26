package org.acme.document.application;

import org.acme.document.application.model.ReadFailure;

/** Error al leer un documento con la IA. El mensaje nunca incluye contenido del documento. */
public class DocumentReadException extends RuntimeException {

    private final ReadFailure failure;

    public DocumentReadException(ReadFailure failure, String message, Throwable cause) {
        super(message, cause);
        this.failure = failure;
    }

    public DocumentReadException(ReadFailure failure, String message) {
        this(failure, message, null);
    }

    public ReadFailure failure() {
        return failure;
    }

    /** Vale la pena reintentar más tarde (corte o demora del servicio). */
    public boolean retryable() {
        return failure == ReadFailure.UNAVAILABLE || failure == ReadFailure.INVALID_RESPONSE;
    }
}
