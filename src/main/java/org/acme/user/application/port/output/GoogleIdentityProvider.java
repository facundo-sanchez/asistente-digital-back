package org.acme.user.application.port.output;

import org.acme.user.application.model.GoogleIdentity;

import java.net.URI;

/** Puerto de salida hacia Google (OAuth 2.0 / OpenID Connect, flujo authorization code + PKCE). */
public interface GoogleIdentityProvider {

    /** URL de Google a la que se redirige al usuario para elegir su cuenta. */
    URI authorizationUrl(String state, String codeChallenge, String redirectUri);

    /**
     * Canjea el código de autorización y devuelve la identidad del id_token verificado.
     * Lanza NotAuthorizedException si Google rechaza el código o el token no es válido,
     * y UnavailableException si no se pudo hablar con Google.
     */
    GoogleIdentity exchangeCode(String code, String codeVerifier, String redirectUri);
}
