-- Módulo 19: link iCal privado por usuario. El token es la clave del link; regenerarlo invalida el anterior.
CREATE TABLE calendar_feeds (
    user_id    UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
