-- Comentarios y sugerencias del usuario, y reportes de errores enviados desde la pantalla de error.
CREATE TABLE feedback (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    kind       VARCHAR(20)   NOT NULL CHECK (kind IN ('SUGGESTION', 'COMMENT', 'PROBLEM', 'ERROR_REPORT')),
    message    VARCHAR(2000) NOT NULL,
    -- Pantalla donde estaba el usuario
    page       VARCHAR(500),
    -- Del reporte de error: mensaje, digest de Next, fecha, etc. (JSON como texto)
    details    TEXT,
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_feedback_user ON feedback (user_id, created_at DESC);
