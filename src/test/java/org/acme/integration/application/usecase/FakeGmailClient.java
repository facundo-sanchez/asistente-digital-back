package org.acme.integration.application.usecase;

import org.acme.integration.application.GmailAccessRevokedException;
import org.acme.integration.application.model.GmailMessage;
import org.acme.integration.application.model.GmailMessagePage;
import org.acme.integration.application.model.GmailTokens;
import org.acme.integration.application.port.output.GmailClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gmail en memoria: los mensajes que "llegaron" y las búsquedas que se hicieron. */
public class FakeGmailClient implements GmailClient {

    public final Map<String, GmailMessage> inbox = new LinkedHashMap<>();
    public final List<String> queries = new ArrayList<>();
    public final List<String> revoked = new ArrayList<>();
    public boolean refreshRevoked;
    public int refreshes;

    @Override
    public URI authorizationUrl(String state, String codeChallenge, String redirectUri) {
        return URI.create("https://google.test/auth?state=" + state + "&redirect_uri=" + redirectUri);
    }

    @Override
    public GmailTokens exchangeCode(String code, String codeVerifier, String redirectUri) {
        return new GmailTokens("access-" + code, "refresh-" + code, Instant.parse("2026-11-26T13:00:00Z"));
    }

    @Override
    public GmailTokens refresh(String refreshToken) {
        if (refreshRevoked) throw new GmailAccessRevokedException("invalid_grant");
        refreshes++;
        return new GmailTokens("access-renovado", null, Instant.parse("2026-11-26T14:00:00Z"));
    }

    @Override
    public void revoke(String token) {
        revoked.add(token);
    }

    @Override
    public String accountEmail(String accessToken) {
        return "ana@gmail.com";
    }

    @Override
    public GmailMessagePage search(String accessToken, String query, String pageToken) {
        queries.add(query);
        return new GmailMessagePage(List.copyOf(inbox.keySet()), null);
    }

    @Override
    public GmailMessage message(String accessToken, String messageId) {
        return inbox.get(messageId);
    }

    @Override
    public byte[] attachment(String accessToken, String messageId, String attachmentId) {
        return ("%PDF-1.4 " + messageId + "/" + attachmentId).getBytes(StandardCharsets.UTF_8);
    }
}
