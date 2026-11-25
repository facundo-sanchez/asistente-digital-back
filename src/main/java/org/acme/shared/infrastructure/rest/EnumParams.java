package org.acme.shared.infrastructure.rest;

import org.acme.domain.exceptions.ServiceException;

/**
 * Conversión de enums que llegan como query param. Se reciben como texto para responder
 * 400 con un mensaje claro: si JAX-RS no puede convertir un enum, responde 404.
 */
public final class EnumParams {

    private EnumParams() {
    }

    /** null o vacío = sin filtro. Un valor desconocido lanza 400 con "<label> inválida: <valor>". */
    public static <E extends Enum<E>> E parse(Class<E> type, String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException e) {
            throw new ServiceException(label + " inválida: " + value);
        }
    }
}
