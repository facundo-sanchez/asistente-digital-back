-- Pagar una factura con la billetera (SPEC-PAGAR-CON-BILLETERA)

-- Billetera elegida por el usuario para pagar facturas (null: ninguna)
ALTER TABLE users ADD COLUMN payment_wallet VARCHAR(30);
ALTER TABLE users ADD CONSTRAINT ck_users_payment_wallet CHECK (payment_wallet IN ('MERCADO_PAGO', 'MODO', 'CUENTA_DNI'));

-- Página donde se pagan las facturas del servicio
ALTER TABLE services ADD COLUMN payment_url VARCHAR(500);
