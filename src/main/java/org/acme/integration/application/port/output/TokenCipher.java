package org.acme.integration.application.port.output;

/** Cifra los tokens de las integraciones antes de guardarlos. */
public interface TokenCipher {

    byte[] encrypt(String plain);

    String decrypt(byte[] encrypted);

    /** Versión de la clave con la que se cifra (para rotarla). */
    String keyVersion();
}
