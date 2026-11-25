package org.acme.user.application.usecase;

import org.acme.shared.application.AppSettings;
import org.acme.shared.application.AuthSettings;
import org.acme.shared.application.EmailDelivery;
import org.acme.shared.application.EmailSettings;

/** Configuración de la aplicación para tests. */
public class TestAppSettings implements AppSettings {

    private final AuthSettings auth = new TestAuthSettings();

    @Override
    public String frontendUrl() {
        return "http://localhost:3000/";
    }

    @Override
    public AuthSettings auth() {
        return auth;
    }

    @Override
    public EmailSettings email() {
        return () -> EmailDelivery.LOG;
    }
}
