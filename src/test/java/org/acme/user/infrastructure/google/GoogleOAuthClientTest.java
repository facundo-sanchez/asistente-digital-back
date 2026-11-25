package org.acme.user.infrastructure.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.acme.domain.exceptions.NotAuthorizedException;
import org.acme.user.application.model.GoogleIdentity;
import org.acme.user.application.usecase.TestAppSettings;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El id_token de Google solo se acepta con firma, audiencia y emisor correctos. */
class GoogleOAuthClientTest {

    private static final String VERIFIER = "v".repeat(43);
    private static final String REDIRECT = "http://localhost:3000/api/auth/google/callback";

    private FakeGoogleServer google;
    private GoogleOAuthClient client;

    @BeforeEach
    void setUp() throws Exception {
        google = new FakeGoogleServer();
        client = new GoogleOAuthClient(new TestAppSettings(new LocalGoogleSettings(google)), new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        google.close();
    }

    @Test
    void aceptaUnTokenValidoYLeeLaIdentidad() throws Exception {
        google.willIssue(claims(LocalGoogleSettings.CLIENT_ID, "https://accounts.google.com"));

        GoogleIdentity identity = client.exchangeCode("codigo", VERIFIER, REDIRECT);

        assertEquals("sub-123", identity.subject());
        assertEquals("ana@gmail.com", identity.email());
        assertTrue(identity.emailVerified());
        assertEquals("Ana", identity.name());
    }

    @Test
    void rechazaUnTokenEmitidoParaOtraAplicacion() throws Exception {
        google.willIssue(claims("client-id-de-otra-app", "https://accounts.google.com"));

        assertThrows(NotAuthorizedException.class, () -> client.exchangeCode("codigo", VERIFIER, REDIRECT));
    }

    @Test
    void rechazaUnTokenDeOtroEmisor() throws Exception {
        google.willIssue(claims(LocalGoogleSettings.CLIENT_ID, "https://evil.example.com"));

        assertThrows(NotAuthorizedException.class, () -> client.exchangeCode("codigo", VERIFIER, REDIRECT));
    }

    private static JwtClaims claims(String audience, String issuer) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(issuer);
        claims.setAudience(audience);
        claims.setSubject("sub-123");
        claims.setIssuedAtToNow();
        claims.setExpirationTimeMinutesInTheFuture(5);
        claims.setClaim("email", "ana@gmail.com");
        claims.setClaim("email_verified", true);
        claims.setClaim("name", "Ana");
        return claims;
    }
}
