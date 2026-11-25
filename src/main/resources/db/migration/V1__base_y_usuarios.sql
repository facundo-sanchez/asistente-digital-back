-- =====================================================================
-- V1 - Base: extensiones, funciones comunes y usuarios
-- Asistente de Vida Digital · PostgreSQL 14+
-- Convenciones (SPEC técnica §5):
--   * PK UUID (gen_random_uuid)
--   * TIMESTAMPTZ para instantes, DATE para fechas de calendario
--   * Importes NUMERIC(19,4) + moneda ISO 4217 CHAR(3)
--   * Enums como VARCHAR + CHECK (mapeables con @Enumerated(STRING))
--   * Todo registro personal tiene user_id (RN-001, RN-014)
-- =====================================================================

-- gen_random_uuid() es nativo desde PostgreSQL 13 (no requiere pgcrypto)
CREATE EXTENSION IF NOT EXISTS citext;   -- emails sin distinguir mayúsculas

-- Actualiza updated_at automáticamente
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- users
-- Login propio (bcrypt) y login con Google, como en el backend actual.
-- Si se adopta Better Auth, password_hash y user_tokens dejan de usarse.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email             CITEXT       NOT NULL,
    name              VARCHAR(150) NOT NULL,
    password_hash     VARCHAR(100),
    google_subject    VARCHAR(255),
    email_verified_at TIMESTAMPTZ,
    timezone          VARCHAR(60)  NOT NULL DEFAULT 'America/Argentina/Buenos_Aires',
    locale            VARCHAR(10)  NOT NULL DEFAULT 'es-AR',
    default_currency  CHAR(3)      NOT NULL DEFAULT 'ARS',
    role              VARCHAR(20)  NOT NULL DEFAULT 'USER',
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_google_subject UNIQUE (google_subject),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_users_auth CHECK (password_hash IS NOT NULL OR google_subject IS NOT NULL)
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- user_tokens: verificación de email, reseteo de contraseña y refresh.
-- Se guarda solo el hash del token, nunca el valor en claro.
-- ---------------------------------------------------------------------
CREATE TABLE user_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_type  VARCHAR(30) NOT NULL,
    token_hash  VARCHAR(128) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_user_tokens_type CHECK (token_type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'REFRESH'))
);

CREATE INDEX ix_user_tokens_user_type ON user_tokens (user_id, token_type);
