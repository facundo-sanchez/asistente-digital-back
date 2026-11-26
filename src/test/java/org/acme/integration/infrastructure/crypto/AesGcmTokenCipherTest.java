package org.acme.integration.infrastructure.crypto;

import org.acme.integration.application.usecase.TestIntegrationSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmTokenCipherTest {

    private final AesGcmTokenCipher cipher = new AesGcmTokenCipher(new TestIntegrationSettings());

    @Test
    void cifraYDescifraConUnIvDistintoCadaVez() {
        byte[] first = cipher.encrypt("ya29.token-secreto");
        byte[] second = cipher.encrypt("ya29.token-secreto");

        assertEquals("ya29.token-secreto", cipher.decrypt(first));
        assertFalse(java.util.Arrays.equals(first, second));
        assertFalse(new String(first).contains("token-secreto"));
    }

    @Test
    void unValorAlteradoNoSeDescifra() {
        byte[] encrypted = cipher.encrypt("ya29.token-secreto");
        encrypted[encrypted.length - 1] ^= 1;

        assertThrows(IllegalStateException.class, () -> cipher.decrypt(encrypted));
    }
}
