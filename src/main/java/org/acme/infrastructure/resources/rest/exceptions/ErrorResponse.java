package org.acme.infrastructure.resources.rest.exceptions;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Error uniforme de la API.
 *
 * @param violations errores por campo (solo en validaciones, 400); null en el resto
 */
public record ErrorResponse(
        String mensaje,
        int status,
        LocalDateTime timestamp,
        List<FieldViolation> violations
) {
    public ErrorResponse(String mensaje, int status, LocalDateTime timestamp) {
        this(mensaje, status, timestamp, null);
    }
}
