package org.acme.notification.application.usecase;

/** Días hacia atrás que la tarea del débito automático todavía registra un pago (lo comparten el débito y los avisos). */
public final class AutoDebitWindow {

    public static final int DAYS = 3;

    private AutoDebitWindow() {
    }
}
