-- =====================================================================
-- V4 - Recurrencias, facturas, obligaciones y pagos
-- (CU-002, CU-005, CU-006, CU-014)
-- Factura, Obligación y Pago son conceptos independientes (SPEC §8).
-- =====================================================================

-- ---------------------------------------------------------------------
-- recurring_rules: obligaciones periódicas detectadas o configuradas
-- ---------------------------------------------------------------------
CREATE TABLE recurring_rules (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    service_id         UUID          REFERENCES services (id) ON DELETE SET NULL,
    asset_id           UUID          REFERENCES assets (id) ON DELETE SET NULL,
    title              VARCHAR(200)  NOT NULL,
    frequency          VARCHAR(30)   NOT NULL,
    day_of_month       SMALLINT,
    next_expected_date DATE          NOT NULL,
    estimated_amount   NUMERIC(19,4),
    currency           CHAR(3),
    origin             VARCHAR(20)   NOT NULL DEFAULT 'USER',
    active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_recurring_frequency CHECK (frequency IN (
        'WEEKLY', 'MONTHLY', 'BIMONTHLY', 'QUARTERLY', 'SEMIANNUAL', 'ANNUAL')),
    CONSTRAINT ck_recurring_origin CHECK (origin IN ('USER', 'DETECTED')),
    CONSTRAINT ck_recurring_day CHECK (day_of_month IS NULL OR day_of_month BETWEEN 1 AND 31),
    CONSTRAINT ck_recurring_amount CHECK (estimated_amount IS NULL OR estimated_amount >= 0),
    CONSTRAINT ck_recurring_currency CHECK ((estimated_amount IS NULL) = (currency IS NULL))
);

CREATE INDEX ix_recurring_user_next ON recurring_rules (user_id, next_expected_date) WHERE active;

CREATE TRIGGER trg_recurring_updated_at
    BEFORE UPDATE ON recurring_rules
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- invoices: factura (dato estructurado). Siempre asociada a un servicio (RN-003).
-- ---------------------------------------------------------------------
CREATE TABLE invoices (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    service_id           UUID          NOT NULL REFERENCES services (id),
    document_id          UUID          REFERENCES documents (id) ON DELETE SET NULL,
    extraction_result_id UUID          REFERENCES ai_extraction_results (id) ON DELETE SET NULL,
    invoice_number       VARCHAR(120),
    billing_period       VARCHAR(30),               -- ej. 2026-12 o 2026-11/2026-12
    issue_date           DATE,
    due_date             DATE          NOT NULL,
    total_amount         NUMERIC(19,4) NOT NULL,
    currency             CHAR(3)       NOT NULL DEFAULT 'ARS',
    payment_code         VARCHAR(200),              -- código de pago / barras
    status               VARCHAR(30)   NOT NULL DEFAULT 'PENDING_VALIDATION',
    source               VARCHAR(30)   NOT NULL DEFAULT 'MANUAL',
    confidence_score     NUMERIC(5,4),
    confirmed_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_invoices_status CHECK (status IN ('PENDING_VALIDATION', 'CONFIRMED', 'REJECTED')),
    CONSTRAINT ck_invoices_source CHECK (source IN ('MANUAL', 'AI_EXTRACTION', 'GMAIL', 'INTEGRATION')),
    CONSTRAINT ck_invoices_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_invoices_confidence CHECK (confidence_score IS NULL OR confidence_score BETWEEN 0 AND 1),
    CONSTRAINT ck_invoices_dates CHECK (issue_date IS NULL OR due_date >= issue_date),
    CONSTRAINT ck_invoices_confirmed CHECK (status <> 'CONFIRMED' OR confirmed_at IS NOT NULL)
);

-- RN-005: la app advierte duplicados; la base impide confirmar dos veces la misma factura
CREATE UNIQUE INDEX uq_invoices_confirmed_number
    ON invoices (user_id, service_id, invoice_number)
    WHERE status = 'CONFIRMED' AND invoice_number IS NOT NULL;
CREATE INDEX ix_invoices_user_status ON invoices (user_id, status);
CREATE INDEX ix_invoices_user_due ON invoices (user_id, due_date);
CREATE INDEX ix_invoices_service ON invoices (service_id);
CREATE INDEX ix_invoices_document ON invoices (document_id);

CREATE TRIGGER trg_invoices_updated_at
    BEFORE UPDATE ON invoices
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- obligations: lo que el usuario debe cumplir. Puede no tener factura.
-- status guarda el estado de negocio; "próxima a vencer" y "vencida"
-- se calculan por fecha (ver vista v_obligations).
-- ---------------------------------------------------------------------
CREATE TABLE obligations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    service_id      UUID          REFERENCES services (id) ON DELETE SET NULL,
    invoice_id      UUID          REFERENCES invoices (id) ON DELETE SET NULL,
    asset_id        UUID          REFERENCES assets (id) ON DELETE SET NULL,
    recurrence_id   UUID          REFERENCES recurring_rules (id) ON DELETE SET NULL,
    title           VARCHAR(200)  NOT NULL,
    description     TEXT,
    obligation_type VARCHAR(50)   NOT NULL DEFAULT 'INVOICE_PAYMENT',
    amount          NUMERIC(19,4),
    currency        CHAR(3),
    due_date        DATE          NOT NULL,
    status          VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    is_estimated    BOOLEAN       NOT NULL DEFAULT FALSE,  -- CU-014: estimada vs. factura real
    cancelled_at    TIMESTAMPTZ,
    cancel_reason   TEXT,
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_obligations_type CHECK (obligation_type IN (
        'INVOICE_PAYMENT', 'TAX', 'INSURANCE', 'VEHICLE_INSPECTION',
        'DOCUMENT_RENEWAL', 'SUBSCRIPTION', 'MAINTENANCE', 'OTHER')),
    CONSTRAINT ck_obligations_status CHECK (status IN (
        'PENDING', 'PARTIALLY_PAID', 'PAID', 'CANCELLED')),
    CONSTRAINT ck_obligations_amount CHECK (amount IS NULL OR amount >= 0),
    CONSTRAINT ck_obligations_currency CHECK ((amount IS NULL) = (currency IS NULL)),
    CONSTRAINT ck_obligations_cancelled CHECK (status <> 'CANCELLED' OR cancelled_at IS NOT NULL),
    CONSTRAINT ck_obligations_estimated_paid CHECK (NOT (is_estimated AND status IN ('PAID', 'PARTIALLY_PAID')))
);

-- Una factura genera como máximo una obligación (evita duplicados)
CREATE UNIQUE INDEX uq_obligations_invoice ON obligations (invoice_id) WHERE invoice_id IS NOT NULL;
CREATE INDEX ix_obligations_user_status_due ON obligations (user_id, status, due_date);
CREATE INDEX ix_obligations_user_due ON obligations (user_id, due_date);
CREATE INDEX ix_obligations_service ON obligations (service_id);
CREATE INDEX ix_obligations_asset ON obligations (asset_id);
CREATE INDEX ix_obligations_recurrence ON obligations (recurrence_id);

CREATE TRIGGER trg_obligations_updated_at
    BEFORE UPDATE ON obligations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Documentos asociados a una obligación (CU-009)
ALTER TABLE documents
    ADD COLUMN obligation_id UUID REFERENCES obligations (id) ON DELETE SET NULL;
CREATE INDEX ix_documents_obligation ON documents (obligation_id);

-- ---------------------------------------------------------------------
-- payments: registro de pagos. Admite pagos parciales.
-- Los pagos no se borran: se anulan (voided_at) para conservar historial (RN-008).
-- ---------------------------------------------------------------------
CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    obligation_id       UUID          NOT NULL REFERENCES obligations (id),
    amount              NUMERIC(19,4) NOT NULL,
    currency            CHAR(3)       NOT NULL,
    payment_date        DATE          NOT NULL,
    payment_method      VARCHAR(40)   NOT NULL DEFAULT 'OTHER',
    receipt_document_id UUID          REFERENCES documents (id) ON DELETE SET NULL,
    notes               TEXT,
    voided_at           TIMESTAMPTZ,
    void_reason         TEXT,
    replaces_payment_id UUID          REFERENCES payments (id),   -- corrección de un pago anterior
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_payments_method CHECK (payment_method IN (
        'CASH', 'DEBIT_CARD', 'CREDIT_CARD', 'BANK_TRANSFER',
        'DIRECT_DEBIT', 'DIGITAL_WALLET', 'OTHER')),
    CONSTRAINT ck_payments_void CHECK ((voided_at IS NULL) = (void_reason IS NULL))
);

CREATE INDEX ix_payments_obligation ON payments (obligation_id) WHERE voided_at IS NULL;
CREATE INDEX ix_payments_user_date ON payments (user_id, payment_date DESC);

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- v_obligations: estado efectivo y saldo para listados y dashboard.
-- "Hoy" se calcula en la zona horaria de cada usuario (RN-012).
-- DUE_SOON = vence dentro de los próximos 7 días.
-- ---------------------------------------------------------------------
CREATE VIEW v_obligations AS
SELECT o.*,
       COALESCE(p.paid_amount, 0)                        AS paid_amount,
       CASE WHEN o.amount IS NULL THEN NULL
            ELSE GREATEST(o.amount - COALESCE(p.paid_amount, 0), 0) END AS balance,
       CASE
           WHEN o.status IN ('PAID', 'CANCELLED') THEN o.status
           WHEN o.due_date < (now() AT TIME ZONE u.timezone)::date THEN 'OVERDUE'
           WHEN o.due_date <= (now() AT TIME ZONE u.timezone)::date + 7 THEN 'DUE_SOON'
           ELSE o.status
       END                                               AS effective_status
FROM obligations o
JOIN users u ON u.id = o.user_id
LEFT JOIN (
    SELECT obligation_id, SUM(amount) AS paid_amount
    FROM payments
    WHERE voided_at IS NULL
    GROUP BY obligation_id
) p ON p.obligation_id = o.id;
