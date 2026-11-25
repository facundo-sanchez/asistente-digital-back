-- =====================================================================
-- V3 - Bienes (CU-010) y documentos (CU-009)
-- Los bienes se crean acá porque los documentos pueden asociarse a uno.
-- =====================================================================

-- ---------------------------------------------------------------------
-- assets: bienes personales (vehículos, propiedades, etc.)
-- ---------------------------------------------------------------------
CREATE TABLE assets (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name          VARCHAR(150) NOT NULL,
    asset_type    VARCHAR(50)  NOT NULL,
    description   TEXT,
    purchase_date DATE,
    metadata      JSONB        NOT NULL DEFAULT '{}'::jsonb,  -- patente, dirección, modelo...
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_assets_type CHECK (asset_type IN (
        'VEHICLE', 'PROPERTY', 'APPLIANCE', 'ELECTRONICS', 'OTHER'))
);

CREATE INDEX ix_assets_user ON assets (user_id, active);

CREATE TRIGGER trg_assets_updated_at
    BEFORE UPDATE ON assets
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- documents: metadatos del archivo. El binario vive en Object Storage.
-- obligation_id se agrega en V4 (la tabla obligations todavía no existe).
-- ---------------------------------------------------------------------
CREATE TABLE documents (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    service_id        UUID         REFERENCES services (id) ON DELETE SET NULL,
    asset_id          UUID         REFERENCES assets (id) ON DELETE SET NULL,
    document_type     VARCHAR(50)  NOT NULL DEFAULT 'OTHER',
    title             VARCHAR(200),
    original_filename VARCHAR(255) NOT NULL,
    storage_key       TEXT         NOT NULL,
    mime_type         VARCHAR(100) NOT NULL,
    file_size         BIGINT       NOT NULL,
    checksum          VARCHAR(128) NOT NULL,          -- SHA-256 en hex
    source            VARCHAR(30)  NOT NULL DEFAULT 'MANUAL',
    processing_status VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    expires_on        DATE,                            -- vencimiento del documento (póliza, licencia...)
    deleted_at        TIMESTAMPTZ,                     -- borrado lógico
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_documents_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_documents_type CHECK (document_type IN (
        'INVOICE', 'CARD_STATEMENT', 'PAYMENT_RECEIPT', 'POLICY',
        'CONTRACT', 'PERSONAL_ID', 'OTHER')),
    CONSTRAINT ck_documents_source CHECK (source IN ('MANUAL', 'GMAIL', 'INTEGRATION')),
    CONSTRAINT ck_documents_processing_status CHECK (processing_status IN (
        'NOT_REQUIRED', 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REQUIRES_REVIEW')),
    CONSTRAINT ck_documents_file_size CHECK (file_size > 0)
);

-- Un mismo archivo no se carga dos veces por el mismo usuario (idempotencia)
CREATE UNIQUE INDEX uq_documents_user_checksum ON documents (user_id, checksum) WHERE deleted_at IS NULL;
CREATE INDEX ix_documents_user_created ON documents (user_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_documents_service ON documents (service_id);
CREATE INDEX ix_documents_asset ON documents (asset_id);
CREATE INDEX ix_documents_expires ON documents (user_id, expires_on) WHERE expires_on IS NOT NULL AND deleted_at IS NULL;

CREATE TRIGGER trg_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- document_processing_jobs: seguimiento de OCR / extracción IA
-- ---------------------------------------------------------------------
CREATE TABLE document_processing_jobs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id   UUID        NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    user_id       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status        VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempts      INTEGER     NOT NULL DEFAULT 0,
    max_attempts  INTEGER     NOT NULL DEFAULT 5,
    provider      VARCHAR(80),
    error_code    VARCHAR(80),
    error_message TEXT,                      -- sin contenido documental sensible
    next_retry_at TIMESTAMPTZ,
    locked_until  TIMESTAMPTZ,               -- recuperación de tareas abandonadas
    started_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at  TIMESTAMPTZ,
    CONSTRAINT ck_doc_jobs_status CHECK (status IN (
        'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REQUIRES_REVIEW', 'DEAD')),
    CONSTRAINT ck_doc_jobs_attempts CHECK (attempts >= 0 AND attempts <= max_attempts)
);

CREATE INDEX ix_doc_jobs_pending ON document_processing_jobs (status, next_retry_at)
    WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX ix_doc_jobs_document ON document_processing_jobs (document_id);

-- ---------------------------------------------------------------------
-- ai_extraction_results: inferencia de la IA, separada de la factura
-- confirmada (RN-006, RN-013). El dato validado vive en invoices.
-- ---------------------------------------------------------------------
CREATE TABLE ai_extraction_results (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id       UUID        NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    job_id            UUID        REFERENCES document_processing_jobs (id) ON DELETE SET NULL,
    user_id           UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    model             VARCHAR(100) NOT NULL,
    schema_version    VARCHAR(20)  NOT NULL,
    detected_type     VARCHAR(50),
    extracted_data    JSONB        NOT NULL,
    confidence_data   JSONB        NOT NULL DEFAULT '{}'::jsonb,
    validation_status VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reviewed_at       TIMESTAMPTZ,
    CONSTRAINT ck_ai_results_validation CHECK (validation_status IN (
        'PENDING', 'CONFIRMED', 'CORRECTED', 'REJECTED'))
);

CREATE INDEX ix_ai_results_document ON ai_extraction_results (document_id, created_at DESC);
CREATE INDEX ix_ai_results_user_pending ON ai_extraction_results (user_id) WHERE validation_status = 'PENDING';
