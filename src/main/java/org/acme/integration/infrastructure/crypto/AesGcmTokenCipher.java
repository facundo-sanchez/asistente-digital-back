package org.acme.integration.infrastructure.crypto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exceptions.UnavailableException;
import org.acme.integration.application.IntegrationSettings;
import org.acme.integration.application.port.output.TokenCipher;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/** AES-256-GCM: cada valor guarda su IV de 12 bytes adelante. La clave viene de INTEGRATIONS_KEY (32 bytes en base64). */
@ApplicationScoped
public class AesGcmTokenCipher implements TokenCipher {

    static final String KEY_VERSION = "v1";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final IntegrationSettings settings;
    private final SecureRandom random = new SecureRandom();

    @Inject
    public AesGcmTokenCipher(IntegrationSettings settings) {
        this.settings = settings;
    }

    @Override
    public byte[] encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(IV_LENGTH + encrypted.length).put(iv).put(encrypted).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el token", e);
        }
    }

    @Override
    public String decrypt(byte[] encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, encrypted, 0, IV_LENGTH));
            return new String(cipher.doFinal(encrypted, IV_LENGTH, encrypted.length - IV_LENGTH), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar el token", e);
        }
    }

    @Override
    public String keyVersion() {
        return KEY_VERSION;
    }

    private SecretKeySpec key() {
        byte[] key = Base64.getDecoder().decode(settings.key().orElseThrow(() -> new UnavailableException("Falta INTEGRATIONS_KEY")));
        if (key.length != 32) {
            throw new IllegalStateException("INTEGRATIONS_KEY tiene que ser de 32 bytes en base64");
        }
        return new SecretKeySpec(key, "AES");
    }
}
