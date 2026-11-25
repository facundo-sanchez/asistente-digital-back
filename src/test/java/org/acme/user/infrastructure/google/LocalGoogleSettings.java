package org.acme.user.infrastructure.google;

import org.acme.shared.application.GoogleSettings;

import java.util.Optional;

/** Login con Google apuntando al {@link FakeGoogleServer}. */
public class LocalGoogleSettings implements GoogleSettings {

    static final String CLIENT_ID = "client-id-de-prueba";

    private final FakeGoogleServer google;

    public LocalGoogleSettings(FakeGoogleServer google) {
        this.google = google;
    }

    @Override
    public Optional<String> clientId() {
        return Optional.of(CLIENT_ID);
    }

    @Override
    public Optional<String> clientSecret() {
        return Optional.of("secreto-de-prueba");
    }

    @Override
    public String authorizationUri() {
        return google.url("/auth");
    }

    @Override
    public String tokenUri() {
        return google.url("/token");
    }

    @Override
    public String jwksUri() {
        return google.url("/certs");
    }
}
