-- Módulo 18: comercios que el usuario marcó como "No es una suscripción" (las suscripciones se calculan, no se guardan).
CREATE TABLE card_subscription_dismissals (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    merchant   VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_card_subscription_dismissals UNIQUE (user_id, merchant)
);
