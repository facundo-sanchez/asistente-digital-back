package org.acme.infrastructure.resources.rest.exceptions;

import io.quarkus.logging.Log;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.LocalDateTime;

/**
 * Errores de base de datos. El detalle técnico va solo al log: el mensaje de la
 * excepción puede exponer tablas, restricciones o datos (SPEC §6.10).
 */
@Provider
public class PersistenceExceptionHandler implements ExceptionMapper<PersistenceException> {

    @Override
    public Response toResponse(PersistenceException exception) {
        Log.error(exception.getMessage(), exception);
        ErrorResponse error = new ErrorResponse(
                "No se pudo completar la operación",
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                LocalDateTime.now()
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
    }
}
