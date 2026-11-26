package org.acme.integration.application.usecase;

import org.acme.integration.application.port.output.TokenCipher;

import java.nio.charset.StandardCharsets;

/** Sin cifrar: el cifrado real se prueba en AesGcmTokenCipherTest. */
public class PlainTokenCipher implements TokenCipher {

    @Override
    public byte[] encrypt(String plain) {
        return plain.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String decrypt(byte[] encrypted) {
        return new String(encrypted, StandardCharsets.UTF_8);
    }

    @Override
    public String keyVersion() {
        return "test";
    }
}
