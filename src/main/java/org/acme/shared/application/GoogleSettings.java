package org.acme.shared.application;

import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Login con Google (prefijo "app.google"). Grupo de {@link AppSettings}.
 * Sin client id / secret el login con Google queda deshabilitado.
 * Las URLs solo se cambian en pruebas (servidor de Google simulado).
 */
public interface GoogleSettings {

    Optional<String> clientId();

    Optional<String> clientSecret();

    @WithDefault("https://accounts.google.com/o/oauth2/v2/auth")
    String authorizationUri();

    @WithDefault("https://oauth2.googleapis.com/token")
    String tokenUri();

    /** Claves públicas con las que Google firma el id_token. */
    @WithDefault("https://www.googleapis.com/oauth2/v3/certs")
    String jwksUri();
}
