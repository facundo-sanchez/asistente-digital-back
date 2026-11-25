package org.acme.user.application.usecase;

import org.acme.shared.application.GoogleSettings;

import java.util.Optional;

/** Login con Google para tests: configurado o sin configurar. */
public class TestGoogleSettings implements GoogleSettings {

    private final boolean configured;

    public TestGoogleSettings(boolean configured) {
        this.configured = configured;
    }

    @Override
    public Optional<String> clientId() {
        return configured ? Optional.of("client-id-de-prueba") : Optional.empty();
    }

    @Override
    public Optional<String> clientSecret() {
        return configured ? Optional.of("secreto-de-prueba") : Optional.empty();
    }

    @Override
    public String authorizationUri() {
        return "https://accounts.google.com/o/oauth2/v2/auth";
    }

    @Override
    public String tokenUri() {
        return "https://oauth2.googleapis.com/token";
    }

    @Override
    public String jwksUri() {
        return "https://www.googleapis.com/oauth2/v3/certs";
    }
}
