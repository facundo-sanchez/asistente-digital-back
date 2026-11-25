package org.acme.notification.domain.model;

/** ALL solo se usa en preferencias, para indicar "todos los tipos". */
public enum NotificationType {
    ALL,
    OBLIGATION_DUE_SOON,
    OBLIGATION_OVERDUE,
    DOCUMENT_DETECTED,
    INVOICE_PENDING_VALIDATION,
    INTEGRATION_DISCONNECTED,
    DOCUMENT_EXPIRING,
    MAINTENANCE_DUE,
    /** Resumen de tarjeta detectado por email sin PDF: invita a subirlo para ver los consumos. */
    CARD_STATEMENT_AVAILABLE
}
