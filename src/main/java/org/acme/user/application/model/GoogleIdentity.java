package org.acme.user.application.model;

/** Datos de la cuenta de Google, tomados del id_token ya verificado. */
public record GoogleIdentity(
        /** Identificador estable de la cuenta de Google (claim "sub"). */
        String subject,
        String email,
        boolean emailVerified,
        /** Puede venir vacío si la cuenta no tiene nombre público. */
        String name
) {}
