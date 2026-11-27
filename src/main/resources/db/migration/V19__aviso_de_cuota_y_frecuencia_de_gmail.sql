-- 13.5: el aviso de cuota de préstamo que manda el banco por email se lee con un patrón (sin IA)
ALTER TABLE provider_email_rules DROP CONSTRAINT ck_provider_email_rules_mode;
ALTER TABLE provider_email_rules ADD CONSTRAINT ck_provider_email_rules_mode CHECK (extraction_mode IN ('ATTACHMENT', 'EMAIL_BODY', 'LOAN_NOTICE'));

-- BBVA: "Mensajes y avisos por e-mail - PRESTAMOS" desde avisos@bbva.com.ar (mismo remitente que el resumen de tarjeta;
-- el asunto los separa y esta regla va primero)
INSERT INTO provider_email_rules (provider_id, sender_address, subject_regex, requires_attachment, document_type, extraction_mode, priority, verified_at)
SELECT p.id, 'avisos@bbva.com.ar', '(?i)PRESTAMOS', FALSE, 'LOAN_RECEIPT', 'LOAN_NOTICE', 5, now()
FROM providers p
WHERE p.code = 'BBVA' AND p.user_id IS NULL;

-- 13.6: cada cuánto se revisa el correo de cada usuario
ALTER TABLE users ADD COLUMN email_check_interval VARCHAR(20) NOT NULL DEFAULT 'EVERY_15_MINUTES';
ALTER TABLE users ADD CONSTRAINT ck_users_email_check_interval CHECK (email_check_interval IN (
    'EVERY_15_MINUTES', 'HOURLY', 'EVERY_6_HOURS', 'DAILY', 'MANUAL'));
