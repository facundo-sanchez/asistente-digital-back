package org.acme.notification.application.model;

/** El servicio de push respondió que la suscripción ya no existe (404 o 410): hay que borrarla. */
public class PushGoneException extends RuntimeException {

    public PushGoneException(String message) {
        super(message);
    }
}
