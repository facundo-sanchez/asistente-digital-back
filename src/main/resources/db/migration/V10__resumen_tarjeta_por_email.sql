-- =====================================================================
-- V10 - Resúmenes de tarjeta detectados por email SIN PDF adjunto
--
-- Caso real (BBVA): el email "Ya podés descargar tu resumen de tarjeta"
-- no adjunta el PDF; trae en el cuerpo el vencimiento, los saldos en pesos
-- y dólares, el pago mínimo y "Número de cuenta ****NNNN". El PDF se baja
-- desde un link que exige iniciar sesión en el home banking.
--
-- Decisiones:
--   * Los datos se extraen del cuerpo con expresiones regulares por proveedor
--     (provider_email_rules.extraction_mode = EMAIL_BODY + body_fields).
--   * El sistema NO sigue links de los emails (requieren login; riesgo de
--     phishing / SSRF, SPEC §7.4). Se notifica al usuario para que suba el PDF
--     si quiere ver los consumos.
--   * El email no informa la fecha de cierre: closing_date pasa a ser opcional
--     y el resumen se identifica por tarjeta + fecha de vencimiento (RN-016).
-- =====================================================================

-- ---------------------------------------------------------------------
-- provider_email_rules: de dónde se extraen los datos
-- ---------------------------------------------------------------------
ALTER TABLE provider_email_rules
    ADD COLUMN extraction_mode VARCHAR(20) NOT NULL DEFAULT 'ATTACHMENT',
    ADD COLUMN body_fields     JSONB       NOT NULL DEFAULT '{}'::jsonb,
    ADD CONSTRAINT ck_provider_email_rules_mode CHECK (extraction_mode IN ('ATTACHMENT', 'EMAIL_BODY')),
    ADD CONSTRAINT ck_provider_email_rules_body_fields CHECK (jsonb_typeof(body_fields) = 'object');

COMMENT ON COLUMN provider_email_rules.body_fields IS
    'Campo -> {"regex": "...", "format": "..."}. El grupo 1 de la regex es el valor. '
    'format: dd-MM-yyyy (fecha), es-AR (importe 1.234,56). Se evalúa sobre el texto del email sin HTML.';

-- ---------------------------------------------------------------------
-- cards: algunos emisores identifican la cuenta, no la tarjeta
-- ---------------------------------------------------------------------
ALTER TABLE cards
    ADD COLUMN account_last_four CHAR(4),
    ADD CONSTRAINT ck_cards_account_last_four CHECK (account_last_four IS NULL OR account_last_four ~ '^[0-9]{4}$');

CREATE INDEX ix_cards_user_account_last_four ON cards (user_id, account_last_four) WHERE account_last_four IS NOT NULL;

-- ---------------------------------------------------------------------
-- card_statements: cierre opcional, clave por vencimiento, trazabilidad del email
-- ---------------------------------------------------------------------
ALTER TABLE card_statements ALTER COLUMN closing_date DROP NOT NULL;

ALTER TABLE card_statements DROP CONSTRAINT ck_card_statements_dates;
ALTER TABLE card_statements ADD CONSTRAINT ck_card_statements_dates CHECK (
    (closing_date IS NULL OR due_date >= closing_date)
    AND (period_start IS NULL OR closing_date IS NULL OR period_start <= closing_date));

ALTER TABLE card_statements ADD COLUMN source_message_id VARCHAR(255);   -- id del email de origen

-- RN-016: un solo resumen confirmado por tarjeta y vencimiento
DROP INDEX uq_card_statements_confirmed;
CREATE UNIQUE INDEX uq_card_statements_confirmed
    ON card_statements (card_id, due_date) WHERE status = 'CONFIRMED';

-- El mismo email no genera dos resúmenes (idempotencia)
CREATE UNIQUE INDEX uq_card_statements_source_message
    ON card_statements (user_id, source_message_id) WHERE source_message_id IS NOT NULL;

DROP INDEX ix_card_statements_card_closing;
CREATE INDEX ix_card_statements_card_due ON card_statements (card_id, due_date DESC);

-- ---------------------------------------------------------------------
-- Vistas: si falta el cierre se usa el vencimiento para ubicar el mes
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_card_spending AS
SELECT t.id,
       t.user_id,
       t.card_id,
       t.statement_id,
       COALESCE(s.closing_date, s.due_date)                         AS closing_date,
       date_trunc('month', COALESCE(s.closing_date, s.due_date))::date AS billing_month,
       t.transaction_date,
       COALESCE(t.merchant_name, t.description)                     AS merchant,
       t.category,
       t.transaction_type,
       t.amount,
       t.currency
FROM card_transactions t
JOIN card_statements s ON s.id = t.statement_id
WHERE s.status = 'CONFIRMED'
  AND t.transaction_type <> 'PAYMENT';

CREATE OR REPLACE VIEW v_card_pending_installments AS
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
           COALESCE(s.closing_date, s.due_date)           AS last_closing_date
    FROM card_transactions t
    JOIN card_statements s ON s.id = t.statement_id
    WHERE s.status = 'CONFIRMED'
      AND t.installment_total > 1
      AND t.amount > 0
    ORDER BY COALESCE(t.purchase_group_id, t.id), t.installment_number DESC, s.due_date DESC
) x
WHERE x.remaining_installments > 0;

-- ---------------------------------------------------------------------
-- Notificación: "tu resumen está disponible, subí el PDF para ver los consumos"
-- ---------------------------------------------------------------------
ALTER TABLE notifications DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notifications_type CHECK (notification_type IN (
    'OBLIGATION_DUE_SOON', 'OBLIGATION_OVERDUE', 'DOCUMENT_DETECTED',
    'INVOICE_PENDING_VALIDATION', 'INTEGRATION_DISCONNECTED',
    'DOCUMENT_EXPIRING', 'MAINTENANCE_DUE', 'CARD_STATEMENT_AVAILABLE'));

-- ---------------------------------------------------------------------
-- Regla BBVA: resumen de tarjeta en el cuerpo del email.
-- Formato validado con un email real (asunto "Ya podes descargar tu resumen de
-- tarjeta Mastercard BBVA"). PENDIENTE: confirmar el remitente exacto; mientras
-- tanto se filtra por dominio y verified_at queda NULL.
-- ---------------------------------------------------------------------
UPDATE providers SET email_domains = ARRAY['bbva.com.ar'], website = 'https://www.bbva.com.ar'
WHERE code = 'BBVA' AND user_id IS NULL;

INSERT INTO provider_email_rules (
    provider_id, subject_regex, requires_attachment, document_type,
    extraction_mode, body_fields, customer_reference_regex, priority)
SELECT p.id,
       '(?i)resumen de (tu )?tarjeta',
       FALSE,
       'CARD_STATEMENT',
       'EMAIL_BODY',
       jsonb_build_object(
           'cardProduct',     jsonb_build_object('regex', '(?i)tarjeta de cr[eé]dito\s+((?:Visa|Mastercard|American Express)[^.\n]*?)\s+ya est[aá]'),
           'accountLastFour', jsonb_build_object('regex', '(?i)N[uú]mero de cuenta\s*\*+\s*([0-9]{4})'),
           'dueDate',         jsonb_build_object('regex', '(?i)Fecha de vencimiento\s*([0-9]{2}-[0-9]{2}-[0-9]{4})', 'format', 'dd-MM-yyyy'),
           'totalAmount',     jsonb_build_object('regex', '(?i)Saldo en pesos\s*\$?\s*([0-9.]+,[0-9]{2})', 'format', 'es-AR'),
           'totalAmountUsd',  jsonb_build_object('regex', '(?i)Saldo en d[oó]lares\s*(?:U\$S|USD)?\s*([0-9.]+,[0-9]{2})', 'format', 'es-AR'),
           'minimumPayment',  jsonb_build_object('regex', '(?i)Pago m[ií]nimo\s*\$?\s*([0-9.]+,[0-9]{2})', 'format', 'es-AR')
       ),
       '(?i)N[uú]mero de cuenta\s*\*+\s*([0-9]{4})',
       10
FROM providers p
WHERE p.code = 'BBVA' AND p.user_id IS NULL;
