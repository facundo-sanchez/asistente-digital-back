-- =====================================================================
-- V5 - Integraciones (CU-003, CU-011, CU-013) y notificaciones (CU-007)
-- =====================================================================

-- ---------------------------------------------------------------------
-- integrations: conexiones externas. Sin tokens (ver integration_credentials).
-- Desconectar no borra el historial obtenido (RN-011): se usa status.
-- ---------------------------------------------------------------------
CREATE TABLE integrations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider            VARCHAR(50)  NOT NULL,
    external_account_id VARCHAR(255) NOT NULL,   -- ej. email de la cuenta Gmail
    status              VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    scopes              JSONB        NOT NULL DEFAULT '[]'::jsonb,
    sync_cursor         VARCHAR(255),            -- historyId de Gmail u otro cursor
    last_sync_at        TIMESTAMPTZ,
    last_error          TEXT,
    disconnected_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_integrations_account UNIQUE (user_id, provider, external_account_id),
    CONSTRAINT ck_integrations_provider CHECK (provider IN ('GMAIL', 'PROVIDER_API')),
    CONSTRAINT ck_integrations_status CHECK (status IN (
        'ACTIVE', 'REAUTH_REQUIRED', 'ERROR', 'DISCONNECTED'))
);

CREATE INDEX ix_integrations_status ON integrations (provider, status);

CREATE TRIGGER trg_integrations_updated_at
    BEFORE UPDATE ON integrations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- integration_credentials: tokens OAuth cifrados por la aplicación
-- ---------------------------------------------------------------------
CREATE TABLE integration_credentials (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    integration_id          UUID        NOT NULL REFERENCES integrations (id) ON DELETE CASCADE,
    encrypted_access_token  BYTEA       NOT NULL,
    encrypted_refresh_token BYTEA,
    access_token_expires_at TIMESTAMPTZ,
    key_version             VARCHAR(30) NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_integration_credentials UNIQUE (integration_id)
);

CREATE TRIGGER trg_integration_credentials_updated_at
    BEFORE UPDATE ON integration_credentials
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- integration_processed_items: deduplicación idempotente (SPEC §8.4)
-- ---------------------------------------------------------------------
CREATE TABLE integration_processed_items (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    integration_id         UUID         NOT NULL REFERENCES integrations (id) ON DELETE CASCADE,
    external_message_id    VARCHAR(255) NOT NULL,
    external_attachment_id VARCHAR(255) NOT NULL DEFAULT '',   -- '' = el mensaje en sí
    document_id            UUID         REFERENCES documents (id) ON DELETE SET NULL,
    result                 VARCHAR(30)  NOT NULL,
    processed_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_integration_items UNIQUE (integration_id, external_message_id, external_attachment_id),
    CONSTRAINT ck_integration_items_result CHECK (result IN (
        'IMPORTED', 'DISCARDED', 'DUPLICATE', 'FAILED'))
);

-- ---------------------------------------------------------------------
-- notification_preferences: por usuario, canal y tipo (RN-009)
-- ---------------------------------------------------------------------
CREATE TABLE notification_preferences (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    channel           VARCHAR(30) NOT NULL,
    notification_type VARCHAR(50) NOT NULL DEFAULT 'ALL',
    enabled           BOOLEAN     NOT NULL DEFAULT TRUE,
    days_before       JSONB       NOT NULL DEFAULT '[7, 3, 1, 0]'::jsonb,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_notification_prefs UNIQUE (user_id, channel, notification_type),
    CONSTRAINT ck_notification_prefs_channel CHECK (channel IN ('EMAIL', 'WEB', 'PUSH')),
    CONSTRAINT ck_notification_prefs_days CHECK (jsonb_typeof(days_before) = 'array')
);

CREATE TRIGGER trg_notification_prefs_updated_at
    BEFORE UPDATE ON notification_preferences
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- notifications: se persisten antes de enviarse; idempotency_key evita duplicados
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    obligation_id     UUID         REFERENCES obligations (id) ON DELETE CASCADE,
    document_id       UUID         REFERENCES documents (id) ON DELETE CASCADE,
    integration_id    UUID         REFERENCES integrations (id) ON DELETE CASCADE,
    notification_type VARCHAR(50)  NOT NULL,
    channel           VARCHAR(30)  NOT NULL,
    title             VARCHAR(200) NOT NULL,
    message           TEXT         NOT NULL,
    idempotency_key   VARCHAR(200) NOT NULL,   -- ej. OBLIGATION_DUE:{id}:{días}:{canal}
    scheduled_at      TIMESTAMPTZ  NOT NULL,
    sent_at           TIMESTAMPTZ,
    read_at           TIMESTAMPTZ,
    status            VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    attempts          INTEGER      NOT NULL DEFAULT 0,
    last_error        TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_notifications_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_notifications_type CHECK (notification_type IN (
        'OBLIGATION_DUE_SOON', 'OBLIGATION_OVERDUE', 'DOCUMENT_DETECTED',
        'INVOICE_PENDING_VALIDATION', 'INTEGRATION_DISCONNECTED',
        'DOCUMENT_EXPIRING', 'MAINTENANCE_DUE')),
    CONSTRAINT ck_notifications_channel CHECK (channel IN ('EMAIL', 'WEB', 'PUSH')),
    CONSTRAINT ck_notifications_status CHECK (status IN (
        'PENDING', 'SENT', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_notifications_attempts CHECK (attempts >= 0)
);

CREATE INDEX ix_notifications_dispatch ON notifications (scheduled_at) WHERE status = 'PENDING';
CREATE INDEX ix_notifications_user_unread ON notifications (user_id, created_at DESC)
    WHERE channel = 'WEB' AND read_at IS NULL;
CREATE INDEX ix_notifications_obligation ON notifications (obligation_id);
