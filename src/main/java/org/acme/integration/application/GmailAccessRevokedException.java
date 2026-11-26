package org.acme.integration.application;

/** Google ya no acepta el permiso (el usuario lo revocó o venció): hay que volver a conectar. */
public class GmailAccessRevokedException extends RuntimeException {

    public GmailAccessRevokedException(String message) {
        super(message);
    }
}
