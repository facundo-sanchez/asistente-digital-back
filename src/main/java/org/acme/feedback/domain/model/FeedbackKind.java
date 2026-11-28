package org.acme.feedback.domain.model;

/** Qué manda el usuario: una sugerencia, un comentario, un problema que encontró o el reporte de un error de la app. */
public enum FeedbackKind {
    SUGGESTION, COMMENT, PROBLEM, ERROR_REPORT
}
