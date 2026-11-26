package org.acme.integration.infrastructure.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;
import org.acme.domain.exceptions.NotAuthorizedException;
import org.acme.domain.exceptions.UnavailableException;
import org.acme.integration.application.GmailAccessRevokedException;
import org.acme.integration.application.IntegrationSettings;
import org.acme.integration.application.model.GmailAttachment;
import org.acme.integration.application.model.GmailMessage;
import org.acme.integration.application.model.GmailMessagePage;
import org.acme.integration.application.model.GmailTokens;
import org.acme.integration.application.port.output.GmailClient;
import org.acme.integration.application.usecase.ConnectGmailUseCase;
import org.acme.shared.application.AppSettings;
import org.acme.shared.application.GoogleSettings;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** OAuth de Google y API de Gmail por HTTP (son pocos endpoints: no hace falta la librería de Google). */
@ApplicationScoped
public class GmailApiClient implements GmailClient {

    private static final Logger LOG = Logger.getLogger(GmailApiClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String REJECTED = "Google no aceptó la conexión. Probá de nuevo.";
    private static final String UNREACHABLE = "No pudimos comunicarnos con Google. Probá en unos minutos.";

    private final GoogleSettings google;
    private final IntegrationSettings settings;
    private final ObjectMapper json;
    private final Clock clock;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    @Inject
    public GmailApiClient(AppSettings app, IntegrationSettings settings, ObjectMapper json, Clock clock) {
        this.google = app.google();
        this.settings = settings;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public URI authorizationUrl(String state, String codeChallenge, String redirectUri) {
        return UriBuilder.fromUri(google.authorizationUri())
                .queryParam("client_id", google.clientId().orElseThrow())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", ConnectGmailUseCase.SCOPE)
                // offline + consent: Google devuelve el refresh token para sincronizar sin el usuario
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent select_account")
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build();
    }

    @Override
    public GmailTokens exchangeCode(String code, String codeVerifier, String redirectUri) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("code_verifier", codeVerifier);
        form.put("redirect_uri", redirectUri);
        HttpResponse<String> response = tokenRequest(form);
        if (response.statusCode() != 200) {
            LOG.warnf("Google rechazó el código de Gmail: HTTP %d", response.statusCode());
            throw new NotAuthorizedException(REJECTED);
        }
        return tokens(response.body());
    }

    @Override
    public GmailTokens refresh(String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", refreshToken);
        HttpResponse<String> response = tokenRequest(form);
        if (response.statusCode() == 400 || response.statusCode() == 401) {
            // invalid_grant: el usuario revocó el permiso o venció
            throw new GmailAccessRevokedException("Google rechazó el refresh token: HTTP " + response.statusCode());
        }
        if (response.statusCode() != 200) {
            throw new UnavailableException(UNREACHABLE);
        }
        return tokens(response.body());
    }

    @Override
    public void revoke(String token) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.revokeUri()))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)))
                .build();
        send(request);
    }

    @Override
    public String accountEmail(String accessToken) {
        return get(accessToken, "/profile").path("emailAddress").asText();
    }

    @Override
    public GmailMessagePage search(String accessToken, String query, String pageToken) {
        UriBuilder uri = UriBuilder.fromUri(settings.gmailApiUri() + "/messages").queryParam("q", query).queryParam("maxResults", 100);
        if (pageToken != null) uri.queryParam("pageToken", pageToken);
        JsonNode body = get(accessToken, uri.build());
        List<String> ids = new ArrayList<>();
        body.path("messages").forEach(m -> ids.add(m.path("id").asText()));
        return new GmailMessagePage(ids, body.hasNonNull("nextPageToken") ? body.get("nextPageToken").asText() : null);
    }

    @Override
    public GmailMessage message(String accessToken, String messageId) {
        JsonNode body = get(accessToken, "/messages/" + messageId + "?format=full");
        JsonNode payload = body.path("payload");
        Map<String, String> headers = new LinkedHashMap<>();
        payload.path("headers").forEach(h -> headers.putIfAbsent(h.path("name").asText().toLowerCase(), h.path("value").asText()));

        List<GmailAttachment> attachments = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        StringBuilder html = new StringBuilder();
        walk(payload, attachments, plain, html);
        String text = !plain.isEmpty() ? plain.toString() : EmailHtml.toText(html.toString());
        Instant received = Instant.ofEpochMilli(body.path("internalDate").asLong(Instant.now(clock).toEpochMilli()));
        return new GmailMessage(messageId, address(headers.get("from")), headers.getOrDefault("subject", ""), received, text, attachments);
    }

    @Override
    public byte[] attachment(String accessToken, String messageId, String attachmentId) {
        return decode(get(accessToken, "/messages/" + messageId + "/attachments/" + attachmentId).path("data").asText());
    }

    /** Recorre las partes del mensaje: junta adjuntos y el cuerpo en texto o en HTML. */
    private static void walk(JsonNode part, List<GmailAttachment> attachments, StringBuilder plain, StringBuilder html) {
        String filename = part.path("filename").asText("");
        String mime = part.path("mimeType").asText("");
        JsonNode body = part.path("body");
        if (!filename.isBlank() && body.hasNonNull("attachmentId")) {
            attachments.add(new GmailAttachment(body.get("attachmentId").asText(), filename, mime));
        } else if (body.hasNonNull("data") && mime.equals("text/plain")) {
            plain.append(new String(decode(body.get("data").asText()), StandardCharsets.UTF_8)).append('\n');
        } else if (body.hasNonNull("data") && mime.equals("text/html")) {
            html.append(new String(decode(body.get("data").asText()), StandardCharsets.UTF_8)).append('\n');
        }
        part.path("parts").forEach(p -> walk(p, attachments, plain, html));
    }

    /** "Camuzzi <factura@factura.camuzzigas.com.ar>" → la dirección. */
    static String address(String from) {
        if (from == null) return null;
        int open = from.lastIndexOf('<');
        int close = from.lastIndexOf('>');
        return (open >= 0 && close > open ? from.substring(open + 1, close) : from).trim().toLowerCase();
    }

    private static byte[] decode(String data) {
        return Base64.getUrlDecoder().decode(data.replace('+', '-').replace('/', '_').replace("=", ""));
    }

    private GmailTokens tokens(String body) {
        try {
            JsonNode node = json.readTree(body);
            Instant expires = Instant.now(clock).plusSeconds(node.path("expires_in").asLong(3600));
            return new GmailTokens(node.path("access_token").asText(), node.hasNonNull("refresh_token") ? node.get("refresh_token").asText() : null,
                    expires);
        } catch (IOException e) {
            throw new NotAuthorizedException(REJECTED);
        }
    }

    private HttpResponse<String> tokenRequest(Map<String, String> form) {
        form.put("client_id", google.clientId().orElseThrow());
        form.put("client_secret", google.clientSecret().orElseThrow());
        String encoded = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return send(HttpRequest.newBuilder(URI.create(google.tokenUri()))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(encoded))
                .build());
    }

    private JsonNode get(String accessToken, String path) {
        return get(accessToken, URI.create(settings.gmailApiUri() + path));
    }

    private JsonNode get(String accessToken, URI uri) {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri).timeout(TIMEOUT).header("Authorization", "Bearer " + accessToken).GET().build());
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new GmailAccessRevokedException("Gmail rechazó el acceso: HTTP " + response.statusCode());
        }
        if (response.statusCode() != 200) {
            LOG.warnf("Gmail respondió HTTP %d", response.statusCode());
            throw new UnavailableException(UNREACHABLE);
        }
        try {
            return json.readTree(response.body());
        } catch (IOException e) {
            throw new UnavailableException(UNREACHABLE);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UnavailableException(UNREACHABLE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnavailableException(UNREACHABLE);
        }
    }
}
