package org.acme.integration.application.port.output;

import org.acme.integration.application.model.GmailMessage;
import org.acme.integration.application.model.GmailMessagePage;
import org.acme.integration.application.model.GmailTokens;

import java.net.URI;

/** Google OAuth + API de Gmail (solo lectura). */
public interface GmailClient {

    URI authorizationUrl(String state, String codeChallenge, String redirectUri);

    GmailTokens exchangeCode(String code, String codeVerifier, String redirectUri);

    /** Lanza GmailAccessRevokedException si Google ya no acepta el refresh token. */
    GmailTokens refresh(String refreshToken);

    /** No falla si Google ya lo había revocado. */
    void revoke(String token);

    /** La dirección de la cuenta conectada. */
    String accountEmail(String accessToken);

    GmailMessagePage search(String accessToken, String query, String pageToken);

    GmailMessage message(String accessToken, String messageId);

    byte[] attachment(String accessToken, String messageId, String attachmentId);
}
