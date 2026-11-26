package org.acme.integration.application.usecase;

import org.acme.integration.application.IntegrationSettings;

import java.util.Optional;

public class TestIntegrationSettings implements IntegrationSettings {

    /** 32 bytes en cero, en base64. */
    public static final String KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Override
    public Optional<String> key() {
        return Optional.of(KEY);
    }

    @Override
    public String gmailApiUri() {
        return "https://gmail.test";
    }

    @Override
    public String revokeUri() {
        return "https://google.test/revoke";
    }

    @Override
    public int maxMessagesPerSync() {
        return 50;
    }
}
