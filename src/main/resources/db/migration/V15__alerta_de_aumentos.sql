-- Módulo 16: aviso cuando una factura viene más cara que las anteriores del mismo servicio

-- Cuánto subió (en %) respecto del promedio de las últimas 3 facturas; null si no subió del umbral o no hay con qué comparar
ALTER TABLE invoices ADD COLUMN price_increase NUMERIC(7, 1);

-- Desde qué porcentaje avisar
ALTER TABLE users ADD COLUMN price_increase_threshold SMALLINT NOT NULL DEFAULT 20;
ALTER TABLE users ADD CONSTRAINT ck_users_price_increase_threshold CHECK (price_increase_threshold IN (10, 20, 30, 50));

ALTER TABLE notifications DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notifications_type CHECK (notification_type IN (
    'OBLIGATION_DUE_SOON', 'OBLIGATION_OVERDUE', 'DOCUMENT_DETECTED',
    'INVOICE_PENDING_VALIDATION', 'INTEGRATION_DISCONNECTED',
    'DOCUMENT_EXPIRING', 'MAINTENANCE_DUE', 'CARD_STATEMENT_AVAILABLE', 'PRICE_INCREASE'));
