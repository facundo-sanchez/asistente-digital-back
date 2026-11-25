package org.acme.user.infrastructure.google;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;

/**
 * Google simulado en un puerto local: /token devuelve el id_token que prepara el test,
 * firmado con una clave propia, y /certs publica esa clave (JWKS).
 */
public class FakeGoogleServer implements AutoCloseable {

    private static final String KEY_ID = "clave-de-prueba";

    private final KeyPair keys;
    private final HttpServer server;
    private String idToken;

    public FakeGoogleServer() throws IOException, NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/token", exchange -> respond(exchange, "{\"id_token\":\"" + idToken + "\"}"));
        server.createContext("/certs", exchange -> respond(exchange, jwks()));
        server.start();
    }

    public String url(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    /** Prepara el id_token que va a devolver /token, firmado con la clave publicada. */
    public void willIssue(JwtClaims claims) throws JoseException {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(keys.getPrivate());
        jws.setKeyIdHeaderValue(KEY_ID);
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        idToken = jws.getCompactSerialization();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private String jwks() {
        try {
            RsaJsonWebKey key = new RsaJsonWebKey((RSAPublicKey) keys.getPublic());
            key.setKeyId(KEY_ID);
            return new JsonWebKeySet(key).toJson();
        } catch (RuntimeException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
