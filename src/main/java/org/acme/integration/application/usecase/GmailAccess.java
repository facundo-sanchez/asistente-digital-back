package org.acme.integration.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.integration.application.GmailAccessRevokedException;
import org.acme.integration.application.model.GmailTokens;
import org.acme.integration.application.port.output.GmailClient;
import org.acme.integration.application.port.output.IntegrationAlerts;
import org.acme.integration.application.port.output.IntegrationCredentialRepository;
import org.acme.integration.application.port.output.IntegrationRepository;
import org.acme.integration.application.port.output.TokenCipher;
import org.acme.integration.domain.model.Integration;
import org.acme.integration.domain.model.IntegrationCredential;
import org.acme.integration.domain.model.IntegrationStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Token de acceso vigente de una cuenta: el guardado, o uno nuevo con el refresh token si está por vencer. */
@ApplicationScoped
public class GmailAccess {

    /** Margen para no usar un token que vence en medio de la sincronización. */
    static final Duration MARGIN = Duration.ofMinutes(2);

    private final GmailClient gmail;
    private final IntegrationRepository integrations;
    private final IntegrationCredentialRepository credentials;
    private final TokenCipher cipher;
    private final IntegrationAlerts alerts;
    private final Clock clock;

    @Inject
    public GmailAccess(GmailClient gmail, IntegrationRepository integrations, IntegrationCredentialRepository credentials, TokenCipher cipher,
                       IntegrationAlerts alerts, Clock clock) {
        this.gmail = gmail;
        this.integrations = integrations;
        this.credentials = credentials;
        this.cipher = cipher;
        this.alerts = alerts;
        this.clock = clock;
    }

    /** Si Google ya no acepta el permiso, la cuenta queda REAUTH_REQUIRED y se lanza GmailAccessRevokedException. */
    public String accessToken(Integration integration) {
        Instant now = Instant.now(clock);
        IntegrationCredential credential = credentials.findByIntegration(integration.getId())
                .orElseThrow(() -> revoked(integration, "Sin credenciales guardadas"));
        if (credential.getAccessTokenExpiresAt() != null && credential.getAccessTokenExpiresAt().isAfter(now.plus(MARGIN))) {
            return cipher.decrypt(credential.getEncryptedAccessToken());
        }
        if (credential.getEncryptedRefreshToken() == null) {
            throw revoked(integration, "Sin refresh token");
        }
        GmailTokens tokens;
        try {
            tokens = gmail.refresh(cipher.decrypt(credential.getEncryptedRefreshToken()));
        } catch (GmailAccessRevokedException e) {
            throw revoked(integration, e.getMessage());
        }
        credential.setEncryptedAccessToken(cipher.encrypt(tokens.accessToken()));
        credential.setAccessTokenExpiresAt(tokens.expiresAt());
        if (tokens.refreshToken() != null) {
            credential.setEncryptedRefreshToken(cipher.encrypt(tokens.refreshToken()));
        }
        credential.setKeyVersion(cipher.keyVersion());
        credentials.save(credential);
        return tokens.accessToken();
    }

    private GmailAccessRevokedException revoked(Integration integration, String reason) {
        integration.setStatus(IntegrationStatus.REAUTH_REQUIRED);
        integration.setLastError(reason);
        integrations.save(integration);
        alerts.reconnectNeeded(integration);
        return new GmailAccessRevokedException(reason);
    }
}
