-- =====================================================================
-- V9 - Tarjetas de crédito: tarjetas, resúmenes, consumos e informe
-- (CU-015, CU-016, CU-017 · RN-016 a RN-020)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Nuevo tipo de obligación para el pago del resumen
-- ---------------------------------------------------------------------
ALTER TABLE obligations DROP CONSTRAINT ck_obligations_type;
ALTER TABLE obligations ADD CONSTRAINT ck_obligations_type CHECK (obligation_type IN (
    'INVOICE_PAYMENT', 'CREDIT_CARD_PAYMENT', 'TAX', 'INSURANCE', 'VEHICLE_INSPECTION',
    'DOCUMENT_RENEWAL', 'SUBSCRIPTION', 'MAINTENANCE', 'OTHER'));

-- ---------------------------------------------------------------------
-- cards: tarjeta de crédito. Pertenece a un servicio CREDIT_CARD del emisor.
-- ---------------------------------------------------------------------
CREATE TABLE cards (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    service_id   UUID          NOT NULL REFERENCES services (id),
    name         VARCHAR(100)  NOT NULL,              -- ej. "Visa Galicia"
    brand        VARCHAR(30)   NOT NULL,
    last_four    CHAR(4),
    holder_name  VARCHAR(150),
    additional   BOOLEAN       NOT NULL DEFAULT FALSE,
    closing_day  SMALLINT,
    due_day      SMALLINT,
    credit_limit NUMERIC(19,4),
    currency     CHAR(3)       NOT NULL DEFAULT 'ARS',
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_cards_brand CHECK (brand IN (
        'VISA', 'MASTERCARD', 'AMEX', 'CABAL', 'NARANJA', 'DINERS', 'OTHER')),
    CONSTRAINT ck_cards_last_four CHECK (last_four IS NULL OR last_four ~ '^[0-9]{4}$'),
    CONSTRAINT ck_cards_closing_day CHECK (closing_day IS NULL OR closing_day BETWEEN 1 AND 31),
    CONSTRAINT ck_cards_due_day CHECK (due_day IS NULL OR due_day BETWEEN 1 AND 31),
    CONSTRAINT ck_cards_limit CHECK (credit_limit IS NULL OR credit_limit >= 0)
);

CREATE UNIQUE INDEX uq_cards_user_brand_last_four
    ON cards (user_id, brand, last_four) WHERE last_four IS NOT NULL;
CREATE INDEX ix_cards_user ON cards (user_id, active);
CREATE INDEX ix_cards_service ON cards (service_id);

CREATE TRIGGER trg_cards_updated_at
    BEFORE UPDATE ON cards
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- card_statements: resumen mensual. Al confirmarse genera una obligación
-- por moneda con saldo (obligation_id en moneda local, obligation_usd_id en USD).
-- ---------------------------------------------------------------------
CREATE TABLE card_statements (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    card_id              UUID          NOT NULL REFERENCES cards (id) ON DELETE CASCADE,
    document_id          UUID          REFERENCES documents (id) ON DELETE SET NULL,
    extraction_result_id UUID          REFERENCES ai_extraction_results (id) ON DELETE SET NULL,
    obligation_id        UUID          REFERENCES obligations (id) ON DELETE SET NULL,
    obligation_usd_id    UUID          REFERENCES obligations (id) ON DELETE SET NULL,
    period_start         DATE,
    closing_date         DATE          NOT NULL,
    due_date             DATE          NOT NULL,
    next_closing_date    DATE,
    next_due_date        DATE,
    currency             CHAR(3)       NOT NULL DEFAULT 'ARS',
    total_amount         NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount_usd     NUMERIC(19,4) NOT NULL DEFAULT 0,
    minimum_payment      NUMERIC(19,4),
    status               VARCHAR(30)   NOT NULL DEFAULT 'PENDING_VALIDATION',
    source               VARCHAR(30)   NOT NULL DEFAULT 'MANUAL',
    confirmed_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_card_statements_status CHECK (status IN ('PENDING_VALIDATION', 'CONFIRMED', 'REJECTED')),
    CONSTRAINT ck_card_statements_source CHECK (source IN ('MANUAL', 'AI_EXTRACTION', 'GMAIL')),
    CONSTRAINT ck_card_statements_amounts CHECK (
        total_amount >= 0 AND total_amount_usd >= 0 AND (minimum_payment IS NULL OR minimum_payment >= 0)),
    CONSTRAINT ck_card_statements_dates CHECK (
        due_date >= closing_date AND (period_start IS NULL OR period_start <= closing_date)),
    CONSTRAINT ck_card_statements_confirmed CHECK (status <> 'CONFIRMED' OR confirmed_at IS NOT NULL)
);

-- RN-016: un solo resumen confirmado por tarjeta y fecha de cierre
CREATE UNIQUE INDEX uq_card_statements_confirmed
    ON card_statements (card_id, closing_date) WHERE status = 'CONFIRMED';
CREATE INDEX ix_card_statements_card_closing ON card_statements (card_id, closing_date DESC);
CREATE INDEX ix_card_statements_user_status ON card_statements (user_id, status);
CREATE INDEX ix_card_statements_document ON card_statements (document_id);

CREATE TRIGGER trg_card_statements_updated_at
    BEFORE UPDATE ON card_statements
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- card_transactions: movimientos del resumen.
-- amount con signo, como figura en el resumen: + cargo, - crédito.
-- ---------------------------------------------------------------------
CREATE TABLE card_transactions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    card_id            UUID          NOT NULL REFERENCES cards (id) ON DELETE CASCADE,
    statement_id       UUID          NOT NULL REFERENCES card_statements (id) ON DELETE CASCADE,
    transaction_date   DATE          NOT NULL,
    description        VARCHAR(255)  NOT NULL,
    merchant_name      VARCHAR(150),
    category           VARCHAR(40)   NOT NULL DEFAULT 'OTHER',
    category_source    VARCHAR(20)   NOT NULL DEFAULT 'AI',
    transaction_type   VARCHAR(30)   NOT NULL DEFAULT 'PURCHASE',
    amount             NUMERIC(19,4) NOT NULL,
    currency           CHAR(3)       NOT NULL,
    installment_number SMALLINT,
    installment_total  SMALLINT,
    original_amount    NUMERIC(19,4),
    purchase_group_id  UUID,
    reference          VARCHAR(100),
    confidence_score   NUMERIC(5,4),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_card_tx_category CHECK (category IN (
        'SUPERMARKET', 'RESTAURANTS', 'FUEL', 'TRANSPORT', 'HEALTH', 'EDUCATION',
        'ENTERTAINMENT', 'TRAVEL', 'CLOTHING', 'HOME', 'TECHNOLOGY', 'SERVICES',
        'SUBSCRIPTIONS', 'TAXES_FEES', 'OTHER')),
    CONSTRAINT ck_card_tx_category_source CHECK (category_source IN ('AI', 'RULE', 'USER')),
    CONSTRAINT ck_card_tx_type CHECK (transaction_type IN (
        'PURCHASE', 'INSTALLMENT', 'REFUND', 'FEE', 'TAX', 'INTEREST', 'PAYMENT', 'ADJUSTMENT')),
    CONSTRAINT ck_card_tx_amount CHECK (amount <> 0),
    CONSTRAINT ck_card_tx_credit_sign CHECK (transaction_type NOT IN ('REFUND', 'PAYMENT') OR amount < 0),
    CONSTRAINT ck_card_tx_installments CHECK (
        (installment_number IS NULL AND installment_total IS NULL)
        OR (installment_total BETWEEN 1 AND 99 AND installment_number BETWEEN 1 AND installment_total)),
    CONSTRAINT ck_card_tx_original_amount CHECK (original_amount IS NULL OR original_amount > 0),
    CONSTRAINT ck_card_tx_confidence CHECK (confidence_score IS NULL OR confidence_score BETWEEN 0 AND 1)
);

CREATE INDEX ix_card_tx_statement ON card_transactions (statement_id);
CREATE INDEX ix_card_tx_user_date ON card_transactions (user_id, transaction_date);
CREATE INDEX ix_card_tx_card_date ON card_transactions (card_id, transaction_date);
CREATE INDEX ix_card_tx_user_category ON card_transactions (user_id, category);
CREATE INDEX ix_card_tx_purchase_group ON card_transactions (purchase_group_id) WHERE purchase_group_id IS NOT NULL;

CREATE TRIGGER trg_card_transactions_updated_at
    BEFORE UPDATE ON card_transactions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- card_merchant_rules: categoría elegida por el usuario para un comercio (RN-019)
-- ---------------------------------------------------------------------
CREATE TABLE card_merchant_rules (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    merchant_name VARCHAR(150) NOT NULL,     -- normalizado (mayúsculas, sin sucursal)
    category      VARCHAR(40)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_card_merchant_rules UNIQUE (user_id, merchant_name),
    CONSTRAINT ck_card_merchant_rules_category CHECK (category IN (
        'SUPERMARKET', 'RESTAURANTS', 'FUEL', 'TRANSPORT', 'HEALTH', 'EDUCATION',
        'ENTERTAINMENT', 'TRAVEL', 'CLOTHING', 'HOME', 'TECHNOLOGY', 'SERVICES',
        'SUBSCRIPTIONS', 'TAXES_FEES', 'OTHER'))
);

CREATE TRIGGER trg_card_merchant_rules_updated_at
    BEFORE UPDATE ON card_merchant_rules
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- v_card_spending: base del informe de compras.
--   * Solo resúmenes confirmados (RN-017).
--   * Excluye los pagos del resumen anterior (no son gasto).
--   * billing_month = mes de la fecha de cierre del resumen.
--   * Importes por moneda, sin conversión (RN-018).
-- ---------------------------------------------------------------------
CREATE VIEW v_card_spending AS
SELECT t.id,
       t.user_id,
       t.card_id,
       t.statement_id,
       s.closing_date,
       date_trunc('month', s.closing_date)::date      AS billing_month,
       t.transaction_date,
       COALESCE(t.merchant_name, t.description)       AS merchant,
       t.category,
       t.transaction_type,
       t.amount,
       t.currency
FROM card_transactions t
JOIN card_statements s ON s.id = t.statement_id
WHERE s.status = 'CONFIRMED'
  AND t.transaction_type <> 'PAYMENT';

-- ---------------------------------------------------------------------
-- v_card_pending_installments: última cuota registrada de cada compra en cuotas
-- con cuotas restantes (RN-020). La compra se identifica por purchase_group_id
-- (o por el propio movimiento si todavía no se vinculó).
-- ---------------------------------------------------------------------
CREATE VIEW v_card_pending_installments AS
SELECT x.*,
       -- cierre estimado del resumen que incluirá la última cuota
       (x.last_closing_date + make_interval(months => x.remaining_installments))::date AS estimated_last_closing_date
FROM (
    SELECT DISTINCT ON (COALESCE(t.purchase_group_id, t.id))
           t.id,
           t.user_id,
           t.card_id,
           t.purchase_group_id,
           t.transaction_date                             AS purchase_date,
           COALESCE(t.merchant_name, t.description)       AS merchant,
           t.category,
           t.currency,
           t.amount                                       AS installment_amount,
           t.installment_number,
           t.installment_total,
           (t.installment_total - t.installment_number)   AS remaining_installments,
           t.amount * (t.installment_total - t.installment_number) AS remaining_amount,
           t.original_amount,
           s.closing_date                                 AS last_closing_date
    FROM card_transactions t
    JOIN card_statements s ON s.id = t.statement_id
    WHERE s.status = 'CONFIRMED'
      AND t.installment_total > 1
      AND t.amount > 0
    ORDER BY COALESCE(t.purchase_group_id, t.id), t.installment_number DESC, s.closing_date DESC
) x
WHERE x.remaining_installments > 0;

-- ---------------------------------------------------------------------
-- Catálogo: emisores de tarjetas (categoría CREDIT_CARD).
-- Sin dominios de email: completarlos con un resumen real de cada emisor
-- antes de habilitar la detección por Gmail.
-- ---------------------------------------------------------------------
INSERT INTO providers (name, code, category) VALUES
    ('Banco Galicia',      'GALICIA',      'CREDIT_CARD'),
    ('Banco Santander',    'SANTANDER',    'CREDIT_CARD'),
    ('BBVA',               'BBVA',         'CREDIT_CARD'),
    ('Banco Nación',       'BNA',          'CREDIT_CARD'),
    ('Banco Provincia',    'BAPRO',        'CREDIT_CARD'),
    ('Banco Macro',        'MACRO',        'CREDIT_CARD'),
    ('Banco Ciudad',       'CIUDAD',       'CREDIT_CARD'),
    ('Naranja X',          'NARANJA_X',    'CREDIT_CARD'),
    ('Mercado Pago',       'MERCADO_PAGO', 'CREDIT_CARD'),
    ('American Express',   'AMEX',         'CREDIT_CARD');
