package org.acme.shared.infrastructure.email;

import org.acme.shared.application.AppSettings;
import org.acme.shared.application.AuthSettings;
import org.acme.shared.application.EmailDelivery;
import org.acme.shared.application.EmailSettings;
import org.acme.shared.application.GoogleSettings;

/** Configuración de prueba con el modo de entrega de emails elegido. */
public class EmailTestSettings implements AppSettings {

    private final EmailDelivery delivery;

    public EmailTestSettings(EmailDelivery delivery) {
        this.delivery = delivery;
    }

    @Override
    public String frontendUrl() {
        return "http://localhost:3000";
    }

    @Override
    public AuthSettings auth() {
        throw new UnsupportedOperationException("No se usa en los tests de email");
    }

    @Override
    public EmailSettings email() {
        return () -> delivery;
    }

    @Override
    public GoogleSettings google() {
        throw new UnsupportedOperationException("No se usa en los tests de email");
    }
}
