-- Módulo 17: servicios que se pagan por débito automático

ALTER TABLE services ADD COLUMN auto_debit BOOLEAN NOT NULL DEFAULT false;
-- Con qué se debita (texto libre, ej. "Visa BBVA" o "Caja de ahorro")
ALTER TABLE services ADD COLUMN auto_debit_method VARCHAR(100);
