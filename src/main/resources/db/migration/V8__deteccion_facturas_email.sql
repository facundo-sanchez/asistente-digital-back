-- =====================================================================
-- V8 - Detección de facturas por email (integración con proveedores vía Gmail)
--
-- Flujo: Gmail (solo lectura) -> filtro por proveedor -> PDF -> extracción
--        -> asociación al servicio por customer_reference -> confirmación del usuario.
--
-- providers.email_domains arma la búsqueda amplia en Gmail
-- (ej. "from:(coopelectric.com.ar) has:attachment filename:pdf").
-- provider_email_rules afina la clasificación de cada mensaje.
-- Las expresiones regulares se evalúan en el backend (sintaxis java.util.regex).
-- =====================================================================

-- ---------------------------------------------------------------------
-- provider_email_rules: cómo reconocer una factura de un proveedor
-- ---------------------------------------------------------------------
CREATE TABLE provider_email_rules (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id              UUID         NOT NULL REFERENCES providers (id) ON DELETE CASCADE,
    sender_address           VARCHAR(255),          -- remitente exacto; NULL = cualquiera de email_domains
    subject_regex            VARCHAR(500),          -- ej. (?i)factura|aviso de deuda
    attachment_name_regex    VARCHAR(500),          -- ej. (?i).*\.pdf$
    requires_attachment      BOOLEAN      NOT NULL DEFAULT TRUE,
    document_type            VARCHAR(50)  NOT NULL DEFAULT 'INVOICE',
    customer_reference_regex VARCHAR(500),          -- extrae nro. de socio/suministro del texto del PDF
    barcode_format           VARCHAR(50),           -- formato del código de pago, si se conoce
    priority                 SMALLINT     NOT NULL DEFAULT 100,  -- menor = se evalúa primero
    active                   BOOLEAN      NOT NULL DEFAULT TRUE,
    verified_at              TIMESTAMPTZ,           -- NULL = regla no validada con una factura real
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_provider_email_rules_doc_type CHECK (document_type IN (
        'INVOICE', 'CARD_STATEMENT', 'PAYMENT_RECEIPT', 'POLICY',
        'CONTRACT', 'PERSONAL_ID', 'OTHER'))
);

CREATE INDEX ix_provider_email_rules_provider ON provider_email_rules (provider_id, priority) WHERE active;

CREATE TRIGGER trg_provider_email_rules_updated_at
    BEFORE UPDATE ON provider_email_rules
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- integrations: desde qué fecha importar al conectar Gmail
-- ---------------------------------------------------------------------
ALTER TABLE integrations
    ADD COLUMN sync_from DATE;   -- ej. hoy - 3 meses; NULL = solo correos nuevos

-- ---------------------------------------------------------------------
-- integration_processed_items: datos para mostrar al usuario
-- "facturas detectadas en tu correo" sin volver a consultar Gmail
-- ---------------------------------------------------------------------
ALTER TABLE integration_processed_items
    ADD COLUMN provider_id   UUID         REFERENCES providers (id) ON DELETE SET NULL,
    ADD COLUMN rule_id       UUID         REFERENCES provider_email_rules (id) ON DELETE SET NULL,
    ADD COLUMN service_id    UUID         REFERENCES services (id) ON DELETE SET NULL,
    ADD COLUMN sender        VARCHAR(255),
    ADD COLUMN subject       VARCHAR(500),
    ADD COLUMN received_at   TIMESTAMPTZ,
    ADD COLUMN discard_reason VARCHAR(100);   -- NOT_MATCHED, NO_ATTACHMENT, UNKNOWN_CUSTOMER...

CREATE INDEX ix_integration_items_integration_received
    ON integration_processed_items (integration_id, received_at DESC);

-- ---------------------------------------------------------------------
-- Reglas iniciales: genéricas por proveedor, pendientes de validar
-- con una factura real de cada uno (verified_at = NULL).
-- ---------------------------------------------------------------------
INSERT INTO provider_email_rules (provider_id, subject_regex, attachment_name_regex, customer_reference_regex)
SELECT p.id,
       '(?i)(factura|aviso de (deuda|vencimiento)|resumen|liquidaci[oó]n)',
       '(?i).*\.pdf$',
       '(?i)(socio|suministro|cliente|cuenta)\s*(n[°ºo]\.?|nro\.?|n[uú]mero)?\s*:?\s*([0-9][0-9./-]{2,20})'
FROM providers p
WHERE p.user_id IS NULL
  AND p.email_domains IS NOT NULL
  AND p.category IN ('ELECTRICITY', 'GAS', 'WATER', 'INTERNET', 'PHONE', 'TV');
