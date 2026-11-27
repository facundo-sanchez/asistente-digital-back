-- Módulo 15: suscripciones a notificaciones push (una por navegador o dispositivo)

CREATE TABLE push_subscriptions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    endpoint   TEXT        NOT NULL UNIQUE,
    p256dh     VARCHAR(200) NOT NULL,
    auth       VARCHAR(100) NOT NULL,
    user_agent VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_push_subscriptions_user ON push_subscriptions (user_id);
