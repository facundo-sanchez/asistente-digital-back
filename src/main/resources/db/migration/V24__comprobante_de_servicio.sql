-- Comprobante de servicio (factura, alta, póliza) para crear un servicio con los datos leídos por la IA

ALTER TABLE documents DROP CONSTRAINT ck_documents_type;
ALTER TABLE documents ADD CONSTRAINT ck_documents_type CHECK (document_type IN (
    'INVOICE', 'CARD_STATEMENT', 'PAYMENT_RECEIPT', 'POLICY', 'CONTRACT', 'PERSONAL_ID',
    'LOAN_CONTRACT', 'LOAN_RECEIPT', 'SERVICE_PROOF', 'OTHER'));

ALTER TABLE provider_email_rules DROP CONSTRAINT ck_provider_email_rules_doc_type;
ALTER TABLE provider_email_rules ADD CONSTRAINT ck_provider_email_rules_doc_type CHECK (document_type IN (
    'INVOICE', 'CARD_STATEMENT', 'PAYMENT_RECEIPT', 'POLICY', 'CONTRACT', 'PERSONAL_ID',
    'LOAN_CONTRACT', 'LOAN_RECEIPT', 'SERVICE_PROOF', 'OTHER'));
