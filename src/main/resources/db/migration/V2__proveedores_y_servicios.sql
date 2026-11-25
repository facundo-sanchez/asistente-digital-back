-- =====================================================================
-- V2 - Proveedores y servicios (CU-001)
-- =====================================================================

-- ---------------------------------------------------------------------
-- providers: catálogo global (user_id NULL) o proveedor propio del usuario
-- ---------------------------------------------------------------------
CREATE TABLE providers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    code        VARCHAR(80)  NOT NULL,
    category    VARCHAR(50)  NOT NULL,
    website     TEXT,
    email_domains TEXT[],     -- dominios remitentes para detectar facturas en Gmail
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_providers_category CHECK (category IN (
        'ELECTRICITY', 'GAS', 'WATER', 'INTERNET', 'PHONE', 'TV',
        'INSURANCE', 'TAX', 'SUBSCRIPTION', 'HEALTH', 'EDUCATION',
        'RENT', 'CREDIT_CARD', 'OTHER'))
);

-- Código único en el catálogo global y único por usuario en los propios
CREATE UNIQUE INDEX uq_providers_global_code ON providers (code) WHERE user_id IS NULL;
CREATE UNIQUE INDEX uq_providers_user_code   ON providers (user_id, code) WHERE user_id IS NOT NULL;
CREATE INDEX ix_providers_category ON providers (category);

CREATE TRIGGER trg_providers_updated_at
    BEFORE UPDATE ON providers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- services: servicio contratado o administrado por el usuario
-- ---------------------------------------------------------------------
CREATE TABLE services (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider_id        UUID         NOT NULL REFERENCES providers (id),
    name               VARCHAR(150) NOT NULL,
    customer_reference VARCHAR(150),  -- número de cliente / contrato
    category           VARCHAR(50)  NOT NULL,
    billing_frequency  VARCHAR(30)  NOT NULL DEFAULT 'MONTHLY',
    start_date         DATE,
    end_date           DATE,
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    notes              TEXT,
    metadata           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_services_category CHECK (category IN (
        'ELECTRICITY', 'GAS', 'WATER', 'INTERNET', 'PHONE', 'TV',
        'INSURANCE', 'TAX', 'SUBSCRIPTION', 'HEALTH', 'EDUCATION',
        'RENT', 'CREDIT_CARD', 'OTHER')),
    CONSTRAINT ck_services_frequency CHECK (billing_frequency IN (
        'NONE', 'WEEKLY', 'MONTHLY', 'BIMONTHLY', 'QUARTERLY',
        'SEMIANNUAL', 'ANNUAL')),
    CONSTRAINT ck_services_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

-- Duplicados: mismo usuario + proveedor + referencia (solo si hay referencia)
CREATE UNIQUE INDEX uq_services_user_provider_ref
    ON services (user_id, provider_id, customer_reference)
    WHERE customer_reference IS NOT NULL;
CREATE INDEX ix_services_user_active ON services (user_id, active);
CREATE INDEX ix_services_provider ON services (provider_id);

CREATE TRIGGER trg_services_updated_at
    BEFORE UPDATE ON services
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
