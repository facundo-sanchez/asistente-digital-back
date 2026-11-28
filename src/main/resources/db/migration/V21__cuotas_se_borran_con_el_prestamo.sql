-- Las cuotas son parte del préstamo: si se borra el préstamo, se borran sus cuotas (con pagos registrados la base no lo deja).
-- Con SET NULL quedaban vencimientos sueltos que se mostraban como vencidos en el inicio.
DELETE FROM payments WHERE obligation_id IN (SELECT id FROM obligations WHERE obligation_type = 'LOAN' AND loan_id IS NULL);
DELETE FROM obligations WHERE obligation_type = 'LOAN' AND loan_id IS NULL;

ALTER TABLE obligations DROP CONSTRAINT IF EXISTS obligations_loan_id_fkey;
ALTER TABLE obligations ADD CONSTRAINT obligations_loan_id_fkey FOREIGN KEY (loan_id) REFERENCES loans (id) ON DELETE CASCADE;
