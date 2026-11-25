package org.acme.user.infrastructure.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.UriBuilder;
import org.acme.domain.exceptions.NotAuthorizedException;
import org.acme.domain.exceptions.UnavailableException;
import org.acme.shared.application.AppSettings;
import org.acme.shared.application.GoogleSettings;
import org.acme.user.application.model.GoogleIdentity;
import org.acme.user.application.port.output.GoogleIdentityProvider;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adaptador de Google OAuth 2.0 / OpenID Connect. El client secret solo vive en el backend.
 * El id_token se verifica con las claves públicas de Google (firma RS256, audiencia = client id,
 * emisor y vencimiento) antes de confiar en su contenido.
 */
@ApplicationScoped
public class GoogleOAuthClient implements GoogleIdentityProvider {

    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String REJECTED = "Google no aceptó el inicio de sesión. Probá de nuevo.";
    private static final String UNREACHABLE = "No pudimos comunicarnos con Google. Probá en unos minutos.";

    private final GoogleSettings settings;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final JWTParser idTokenParser;

    @Inject
    public GoogleOAuthClient(AppSettings appSettings, ObjectMapper json) {
        this.settings = appSettings.google();
        this.json = json;
        this.idTokenParser = new DefaultJWTParser(idTokenRules(settings));
    }

    @Override
    public URI authorizationUrl(String state, String codeChallenge, String redirectUri) {
        return UriBuilder.fromUri(settings.authorizationUri())
                .queryParam("client_id", clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .queryParam("prompt", "select_account")
                .build();
    }

    @Override
    public GoogleIdentity exchangeCode(String code, String codeVerifier, String redirectUri) {
        JsonWebToken token = verify(requestIdToken(code, codeVerifier, redirectUri));
        return new GoogleIdentity(
                token.getSubject(),
                stringClaim(token, "email"),
                booleanClaim(token, "email_verified"),
                stringClaim(token, "name"));
    }

    private String requestIdToken(String code, String codeVerifier, String redirectUri) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("code_verifier", codeVerifier);
        form.put("redirect_uri", redirectUri);
        form.put("client_id", clientId());
        form.put("client_secret", settings.clientSecret().orElseThrow());

        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.tokenUri()))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(form)))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            Log.errorf(e, "No se pudo llamar al token endpoint de Google");
            throw new UnavailableException(UNREACHABLE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnavailableException(UNREACHABLE);
        }

        if (response.statusCode() != 200) {
            // El cuerpo de error de Google no trae secretos (ej. {"error":"invalid_grant"})
            Log.warnf("Google rechazó el código de autorización: HTTP %d %s", response.statusCode(), response.body());
            throw new NotAuthorizedException(REJECTED);
        }
        return readIdToken(response.body());
    }

    private String readIdToken(String body) {
        String idToken;
        try {
            idToken = json.readValue(body, GoogleTokenResponse.class).idToken();
        } catch (IOException e) {
            Log.warn("Respuesta inválida del token endpoint de Google");
            throw new NotAuthorizedException(REJECTED);
        }
        if (idToken == null) {
            Log.warn("El token endpoint de Google no devolvió id_token");
            throw new NotAuthorizedException(REJECTED);
        }
        return idToken;
    }

    private JsonWebToken verify(String idToken) {
        try {
            JsonWebToken token = idTokenParser.parse(idToken);
            if (!GOOGLE_ISSUERS.contains(token.getIssuer())) {
                throw new ParseException("Emisor inesperado: " + token.getIssuer());
            }
            return token;
        } catch (ParseException e) {
            Log.warnf("id_token de Google inválido: %s", e.getMessage());
            throw new NotAuthorizedException(REJECTED);
        }
    }

    /** Reglas de validación del id_token. El emisor se valida aparte porque Google usa dos valores. */
    private static JWTAuthContextInfo idTokenRules(GoogleSettings settings) {
        JWTAuthContextInfo rules = new JWTAuthContextInfo();
        rules.setPublicKeyLocation(settings.jwksUri());
        rules.setSignatureAlgorithm(Set.of(SignatureAlgorithm.RS256));
        // Sin client id configurado ningún token coincide (el caso de uso igual no llega acá)
        rules.setExpectedAudience(Set.of(settings.clientId().orElse("")));
        rules.setRequiredClaims(Set.of("sub", "email"));
        return rules;
    }

    private String clientId() {
        return settings.clientId().orElseThrow();
    }

    /** El claim puede llegar como Boolean o como JsonValue.TRUE / FALSE según el parser. */
    private static boolean booleanClaim(JsonWebToken token, String name) {
        Object value = token.getClaim(name);
        return value instanceof Boolean bool ? bool : JsonValue.TRUE.equals(value);
    }

    private static String stringClaim(JsonWebToken token, String name) {
        Object value = token.getClaim(name);
        if (value instanceof JsonString jsonString) {
            return jsonString.getString();
        }
        return value == null ? null : value.toString();
    }

    private static String formEncode(Map<String, String> form) {
        return form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
