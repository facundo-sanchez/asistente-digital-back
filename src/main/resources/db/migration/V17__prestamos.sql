-- Módulo 13: préstamos tomados. Cada cuota es un vencimiento (obligations) de tipo LOAN.

CREATE TABLE loans (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    lender               VARCHAR(150)  NOT NULL,              -- banco, financiera o persona
    name                 VARCHAR(150)  NOT NULL,              -- "Préstamo personal"
    loan_number          VARCHAR(60),                         -- número del comprobante del banco
    short_reference      VARCHAR(60),                         -- la referencia corta de los avisos por email (ej. "203/2")
    currency             CHAR(3)       NOT NULL,
    principal            NUMERIC(19,4),                       -- capital pedido
    rate_type            VARCHAR(20),                         -- FIXED / VARIABLE
    annual_rate          NUMERIC(7,3),                        -- TNA en %
    disbursement_date    DATE,
    installments_count   SMALLINT      NOT NULL,
    first_due_date       DATE          NOT NULL,
    contract_document_id UUID          REFERENCES documents (id) ON DELETE SET NULL,
    bank_balance         NUMERIC(19,4),                       -- último saldo de capital informado por el banco
    bank_balance_date    DATE,
    notes                TEXT,
    status               VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    cancel_reason        VARCHAR(500),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_loans_currency CHECK (currency IN ('ARS', 'USD')),
    CONSTRAINT ck_loans_installments CHECK (installments_count BETWEEN 1 AND 360),
    CONSTRAINT ck_loans_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT ck_loans_rate_type CHECK (rate_type IS NULL OR rate_type IN ('FIXED', 'VARIABLE')),
    CONSTRAINT uq_loans_number UNIQUE (user_id, loan_number)
);

CREATE INDEX ix_loans_user ON loans (user_id, status);

CREATE TRIGGER trg_loans_updated_at
    BEFORE UPDATE ON loans
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- La cuota: a qué préstamo pertenece y qué número es
ALTER TABLE obligations ADD COLUMN loan_id UUID REFERENCES loans (id) ON DELETE SET NULL;
ALTER TABLE obligations ADD COLUMN installment_number SMALLINT;
ALTER TABLE obligations ADD CONSTRAINT uq_obligations_loan_installment UNIQUE (loan_id, installment_number);
CREATE INDEX ix_obligations_loan ON obligations (loan_id) WHERE loan_id IS NOT NULL;

ALTER TABLE obligations DROP CONSTRAINT ck_obligations_type;
ALTER TABLE obligations ADD CONSTRAINT ck_obligations_type CHECK (obligation_type IN (
    'INVOICE_PAYMENT', 'CREDIT_CARD_PAYMENT', 'TAX', 'INSURANCE', 'VEHICLE_INSPECTION',
    'DOCUMENT_RENEWAL', 'SUBSCRIPTION', 'MAINTENANCE', 'RENT', 'LOAN', 'OTHER'));

-- La vista se creó con o.*: Postgres fija las columnas al crearla, así que se recrea para que vea las nuevas
DROP VIEW v_obligations;
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
