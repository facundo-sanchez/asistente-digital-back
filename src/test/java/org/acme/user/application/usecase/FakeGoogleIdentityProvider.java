package org.acme.user.application.usecase;

import org.acme.domain.exceptions.NotAuthorizedException;
import org.acme.user.application.model.GoogleIdentity;
import org.acme.user.application.port.output.GoogleIdentityProvider;

import java.net.URI;

/** Google simulado: el código "codigo-valido" devuelve la identidad configurada. */
public class FakeGoogleIdentityProvider implements GoogleIdentityProvider {

    static final String VALID_CODE = "codigo-valido";

    private GoogleIdentity identity;
    private String lastRedirectUri;

    public void willReturn(GoogleIdentity identity) {
        this.identity = identity;
    }

    public String lastRedirectUri() {
        return lastRedirectUri;
    }

    @Override
    public URI authorizationUrl(String state, String codeChallenge, String redirectUri) {
        lastRedirectUri = redirectUri;
        return URI.create("https://accounts.google.com/o/oauth2/v2/auth?state=" + state);
    }

    @Override
    public GoogleIdentity exchangeCode(String code, String codeVerifier, String redirectUri) {
        lastRedirectUri = redirectUri;
        if (!VALID_CODE.equals(code)) {
            throw new NotAuthorizedException("Código inválido");
        }
        return identity;
    }
}
