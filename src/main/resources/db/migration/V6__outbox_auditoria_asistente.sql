-- =====================================================================
-- V6 - Outbox (procesamiento asíncrono), auditoría (RN-015) y asistente IA (CU-008)
-- =====================================================================

-- ---------------------------------------------------------------------
-- outbox_events: Transactional Outbox (SPEC §9.3)
-- Se inserta en la misma transacción que el cambio de negocio.
-- Los workers toman eventos con SELECT ... FOR UPDATE SKIP LOCKED.
-- ---------------------------------------------------------------------
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(80)  NOT NULL,   -- DOCUMENT, INVOICE, OBLIGATION...
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(100) NOT NULL,   -- DOCUMENT_UPLOADED, INVOICE_CONFIRMED...
    user_id        UUID         REFERENCES users (id) ON DELETE CASCADE,
    payload        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts       INTEGER      NOT NULL DEFAULT 0,
    available_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),  -- backoff: próximo intento
    locked_until   TIMESTAMPTZ,
    last_error     TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at   TIMESTAMPTZ,
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'DONE', 'FAILED', 'DEAD')),
    CONSTRAINT ck_outbox_attempts CHECK (attempts >= 0)
);

CREATE INDEX ix_outbox_pending ON outbox_events (available_at)
    WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX ix_outbox_aggregate ON outbox_events (aggregate_type, aggregate_id);

-- ---------------------------------------------------------------------
-- audit_logs: sin tokens, contraseñas ni contenido documental
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         REFERENCES users (id) ON DELETE SET NULL,
    action         VARCHAR(100) NOT NULL,   -- PAYMENT_REGISTERED, INTEGRATION_DISCONNECTED...
    entity_type    VARCHAR(80)  NOT NULL,
    entity_id      UUID,
    metadata       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    correlation_id VARCHAR(100),
    ip_address     INET,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_audit_user_created ON audit_logs (user_id, created_at DESC);
CREATE INDEX ix_audit_entity ON audit_logs (entity_type, entity_id);

-- ---------------------------------------------------------------------
-- Asistente conversacional
-- ---------------------------------------------------------------------
CREATE TABLE assistant_conversations (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title      VARCHAR(200),
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_assistant_conv_user ON assistant_conversations (user_id, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_assistant_conv_updated_at
    BEFORE UPDATE ON assistant_conversations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE assistant_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID        NOT NULL REFERENCES assistant_conversations (id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    content         TEXT        NOT NULL,
    metadata        JSONB       NOT NULL DEFAULT '{}'::jsonb,  -- tools usadas, referencias, tokens
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_assistant_messages_role CHECK (role IN ('USER', 'ASSISTANT', 'TOOL', 'SYSTEM'))
);

CREATE INDEX ix_assistant_messages_conv ON assistant_messages (conversation_id, created_at);
