-- Parte 4.6: vencimientos que se repiten (alquiler, seguros, VTV).

-- Tipo "Alquiler" para las obligaciones
ALTER TABLE obligations DROP CONSTRAINT ck_obligations_type;
ALTER TABLE obligations ADD CONSTRAINT ck_obligations_type CHECK (obligation_type IN (
    'INVOICE_PAYMENT', 'CREDIT_CARD_PAYMENT', 'TAX', 'INSURANCE', 'VEHICLE_INSPECTION',
    'DOCUMENT_RENEWAL', 'SUBSCRIPTION', 'MAINTENANCE', 'RENT', 'OTHER'));

-- Tipo de los vencimientos que genera cada regla
ALTER TABLE recurring_rules ADD COLUMN obligation_type VARCHAR(50) NOT NULL DEFAULT 'OTHER';
ALTER TABLE recurring_rules ADD CONSTRAINT ck_recurring_obligation_type CHECK (obligation_type IN (
    'INVOICE_PAYMENT', 'CREDIT_CARD_PAYMENT', 'TAX', 'INSURANCE', 'VEHICLE_INSPECTION',
    'DOCUMENT_RENEWAL', 'SUBSCRIPTION', 'MAINTENANCE', 'RENT', 'OTHER'));

-- Un solo vencimiento por regla y fecha: la tarea diaria nunca duplica un período
CREATE UNIQUE INDEX uq_obligations_recurrence_due ON obligations (recurrence_id, due_date) WHERE recurrence_id IS NOT NULL;
