package org.acme.integration.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.domain.exceptions.UnavailableException;
import org.acme.integration.application.IntegrationSettings;
import org.acme.integration.application.model.GmailTokens;
import org.acme.integration.application.port.output.GmailClient;
import org.acme.integration.application.port.output.IntegrationCredentialRepository;
import org.acme.integration.application.port.output.IntegrationRepository;
import org.acme.integration.application.port.output.TokenCipher;
import org.acme.integration.domain.model.Integration;
import org.acme.integration.domain.model.IntegrationCredential;
import org.acme.integration.domain.model.IntegrationProvider;
import org.acme.integration.domain.model.IntegrationStatus;
import org.acme.shared.application.AppSettings;
import org.jboss.logging.Logger;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Conectar, listar y desconectar Gmail (CU-003, CU-013). Desconectar no borra lo importado (RN-011). */
@ApplicationScoped
public class ConnectGmailUseCase {

    /** Ruta del BFF que recibe la respuesta de Google. Debe estar registrada en Google Cloud. */
    public static final String CALLBACK_PATH = "/api/integrations/gmail/callback";
    public static final String SCOPE = "https://www.googleapis.com/auth/gmail.readonly";

    static final String NOT_CONFIGURED = "Conectar Gmail no está disponible";
    static final String NO_OFFLINE_ACCESS = "Google no dio acceso permanente. Probá conectar de nuevo.";

    private static final Logger LOG = Logger.getLogger(ConnectGmailUseCase.class);

    private final GmailClient gmail;
    private final IntegrationRepository integrations;
    private final IntegrationCredentialRepository credentials;
    private final TokenCipher cipher;
    private final AppSettings app;
    private final IntegrationSettings settings;
    private final Clock clock;

    @Inject
    public ConnectGmailUseCase(GmailClient gmail, IntegrationRepository integrations, IntegrationCredentialRepository credentials,
                               TokenCipher cipher, AppSettings app, IntegrationSettings settings, Clock clock) {
        this.gmail = gmail;
        this.integrations = integrations;
        this.credentials = credentials;
        this.cipher = cipher;
        this.app = app;
        this.settings = settings;
        this.clock = clock;
    }

    /** URL de Google para dar el permiso. state y codeChallenge los genera el BFF. */
    public URI authorizationUrl(String state, String codeChallenge) {
        ensureConfigured();
        return gmail.authorizationUrl(state, codeChallenge, redirectUri());
    }

    /** Canjea el código y guarda la cuenta (o la reactiva si ya estaba). syncFrom null = solo los correos nuevos. */
    @Transactional
    public Integration connect(UUID userId, String code, String codeVerifier, LocalDate syncFrom) {
        ensureConfigured();
        GmailTokens tokens = gmail.exchangeCode(code, codeVerifier, redirectUri());
        if (tokens.refreshToken() == null) {
            throw new ServiceException(NO_OFFLINE_ACCESS);
        }
        String account = gmail.accountEmail(tokens.accessToken());
        Integration integration = integrations.findByAccount(userId, IntegrationProvider.GMAIL, account)
                .orElseGet(() -> Integration.builder().userId(userId).provider(IntegrationProvider.GMAIL).externalAccountId(account).build());
        integration.setStatus(IntegrationStatus.ACTIVE);
        integration.setScopes(new ArrayList<>(List.of(SCOPE)));
        integration.setSyncFrom(syncFrom);
        integration.setLastError(null);
        integration.setDisconnectedAt(null);
        Integration saved = integrations.save(integration);

        IntegrationCredential credential = credentials.findByIntegration(saved.getId())
                .orElseGet(() -> IntegrationCredential.builder().integrationId(saved.getId()).build());
        credential.setEncryptedAccessToken(cipher.encrypt(tokens.accessToken()));
        credential.setEncryptedRefreshToken(cipher.encrypt(tokens.refreshToken()));
        credential.setAccessTokenExpiresAt(tokens.expiresAt());
        credential.setKeyVersion(cipher.keyVersion());
        credentials.save(credential);
        return saved;
    }

    /** Las cuentas conectadas o que piden reconectar (las desconectadas no). */
    public List<Integration> list(UUID userId) {
        return integrations.findForUser(userId).stream().filter(i -> i.getStatus() != IntegrationStatus.DISCONNECTED).toList();
    }

    @Transactional
    public void disconnect(UUID userId, UUID integrationId) {
        Integration integration = integrations.findByIdForUser(userId, integrationId).orElseThrow(() -> new NotFoundException("integración"));
        credentials.findByIntegration(integrationId).ifPresent(credential -> {
            try {
                byte[] token = credential.getEncryptedRefreshToken() != null ? credential.getEncryptedRefreshToken() : credential.getEncryptedAccessToken();
                gmail.revoke(cipher.decrypt(token));
            } catch (RuntimeException e) {
                // Si Google no responde igual se desconecta: los tokens se borran y no se vuelven a usar
                LOG.warnf("No se pudo revocar el permiso de Gmail de %s: %s", integrationId, e.getClass().getSimpleName());
            }
        });
        credentials.deleteByIntegration(integrationId);
        integration.disconnect(Instant.now(clock));
        integrations.save(integration);
    }

    private void ensureConfigured() {
        if (app.google().clientId().isEmpty() || app.google().clientSecret().isEmpty() || settings.key().isEmpty()) {
            throw new UnavailableException(NOT_CONFIGURED);
        }
    }

    private String redirectUri() {
        return app.frontendUrl().replaceAll("/+$", "") + CALLBACK_PATH;
    }
}
