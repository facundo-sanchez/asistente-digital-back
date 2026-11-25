-- =====================================================================
-- V11 - Reglas de detección por email verificadas con emails reales
-- =====================================================================
-- El usuario no carga remitentes ni reglas: vienen del catálogo y se aplican solas.
-- Con emails reales (25/11/2026) se vio que:
--   * Coopelectric, Camuzzi y Personal mandan todo en el cuerpo, sin PDF adjunto
--     (las reglas genéricas de V8 esperaban un PDF).
--   * Camuzzi y Personal envían desde subdominios (factura.camuzzigas.com.ar,
--     email.personal.com.ar): el remitente coincide con un proveedor si su dominio es
--     igual o subdominio de providers.email_domains, o si es exactamente sender_address.
--   * Coopelectric puede traer varias facturas en un mismo email (una fila por
--     vencimiento): se extraen con un campo de filas (body_fields.<campo>.groups).
--   * Personal no informa ningún número estable (el "referente de pago" cambia cada
--     mes): customer_reference_regex queda NULL y la factura se asocia por remitente
--     (si el usuario tiene un solo servicio de ese proveedor) o el usuario elige.
-- Asociación de la factura al servicio (en este orden):
--   1. customer_reference_regex encuentra el nro. y coincide con services.customer_reference
--      (comparación sin espacios, guiones, barras ni mayúsculas).
--   2. Sin nro.: el usuario tiene un solo servicio activo del proveedor.
--   3. Si no: la factura queda detectada y el usuario elige el servicio al confirmar.
-- =====================================================================

COMMENT ON COLUMN provider_email_rules.body_fields IS
    'Campo -> {"regex", "format"} (valor simple, grupo 1) o {"regex", "groups"} (filas que se repiten). '
    'format: dd-MM-yyyy o dd/MM/yyyy (fecha), es-AR (1.234,56), decimal (1234.56). '
    'groups: "nombre[:formato],..." en el orden de los grupos. Se evalúa sobre asunto + cuerpo sin HTML.';

COMMENT ON COLUMN provider_email_rules.customer_reference_regex IS
    'Grupo 1 = nro. de cliente / suministro / cuenta, sobre asunto + cuerpo. NULL: el email no '
    'trae un nro. estable; se asocia por remitente o eligiendo el servicio.';

-- ---------------------------------------------------------------------
-- Coopelectric: "Nueva factura disponible", tabla Suministro | Dirección | Vencimiento | Total
-- ---------------------------------------------------------------------
UPDATE provider_email_rules r SET
    sender_address           = 'registroweb@coopelectric.com.ar',
    subject_regex            = '(?i)nueva factura disponible',
    attachment_name_regex    = NULL,
    requires_attachment      = FALSE,
    extraction_mode          = 'EMAIL_BODY',
    customer_reference_regex = NULL,   -- viene en cada fila
    body_fields              = jsonb_build_object(
        'invoices', jsonb_build_object(
            'regex',  '(?i)([0-9]{4,12})\s+.+?\s+([0-9]{2}/[0-9]{2}/[0-9]{4})\s+\$\s*([0-9]+\.[0-9]{2})',
            'groups', 'customerReference,dueDate:dd/MM/yyyy,totalAmount:decimal')),
    verified_at              = now()
FROM providers p
WHERE p.id = r.provider_id AND p.code = 'COOPELECTRIC' AND p.user_id IS NULL;

-- ---------------------------------------------------------------------
-- Camuzzi: el nro. de cuenta viene en el asunto; total y vencimiento en el cuerpo
-- ---------------------------------------------------------------------
UPDATE provider_email_rules r SET
    sender_address           = 'factura@factura.camuzzigas.com.ar',
    subject_regex            = '(?i)camuzzi te acerca tu factura',
    attachment_name_regex    = NULL,
    requires_attachment      = FALSE,
    extraction_mode          = 'EMAIL_BODY',
    customer_reference_regex = '(?i)Nro\.?\s*Cuenta:?\s*([0-9][0-9/-]*[0-9])',
    body_fields              = jsonb_build_object(
        'invoiceNumber', jsonb_build_object('regex', '(?i)Factura\s+([0-9][0-9/-]*[0-9])'),
        'totalAmount',   jsonb_build_object('regex', '(?i)Total:\s*\$\s*([0-9.]+,[0-9]{2})', 'format', 'es-AR'),
        'dueDate',       jsonb_build_object('regex', '(?i)Vencimiento:\s*([0-9]{2}/[0-9]{2}/[0-9]{4})', 'format', 'dd/MM/yyyy'),
        'period',        jsonb_build_object('regex', '(?i)per[ií]odo\s+([0-9]{2}/[0-9]{2})')),
    verified_at              = now()
FROM providers p
WHERE p.id = r.provider_id AND p.code = 'CAMUZZI' AND p.user_id IS NULL;

-- ---------------------------------------------------------------------
-- Personal: sin nro. estable; total, vencimiento y referente de pago (cambia cada mes)
-- ---------------------------------------------------------------------
UPDATE provider_email_rules r SET
    sender_address           = 'facturacion@email.personal.com.ar',
    subject_regex            = '(?i)saldo total a debitar',
    attachment_name_regex    = NULL,
    requires_attachment      = FALSE,
    extraction_mode          = 'EMAIL_BODY',
    customer_reference_regex = NULL,
    body_fields              = jsonb_build_object(
        'totalAmount',      jsonb_build_object('regex', '(?i)Total a debitar\s*\$\s*([0-9.]+,[0-9]{2})', 'format', 'es-AR'),
        'dueDate',          jsonb_build_object('regex', '(?i)Vencimiento\s*([0-9]{2}/[0-9]{2}/[0-9]{4})', 'format', 'dd/MM/yyyy'),
        'paymentReference', jsonb_build_object('regex', '(?i)Referente de pago\s*([0-9]+)')),
    verified_at              = now()
FROM providers p
WHERE p.id = r.provider_id AND p.code = 'PERSONAL' AND p.user_id IS NULL;

-- ---------------------------------------------------------------------
-- BBVA: la regla de V10 ya extrae del cuerpo; falta el remitente exacto
-- ---------------------------------------------------------------------
UPDATE provider_email_rules r SET
    sender_address = 'avisos@bbva.com.ar',
    verified_at    = now()
FROM providers p
WHERE p.id = r.provider_id AND p.code = 'BBVA' AND p.user_id IS NULL
  AND r.document_type = 'CARD_STATEMENT';
