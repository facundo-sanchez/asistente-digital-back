package org.acme.shared.application;

/** Cómo se entregan los emails de la aplicación. */
public enum EmailDelivery {

    /** Se escriben en el log, no se envían. Solo para desarrollo. */
    LOG,

    /** Se envían por SMTP (Mailpit en desarrollo, Gmail u otro proveedor en producción). */
    SMTP
}
