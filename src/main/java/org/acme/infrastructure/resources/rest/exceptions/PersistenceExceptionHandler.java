package org.acme.infrastructure.resources.rest.exceptions;

import io.quarkus.logging.Log;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.LocalDateTime;

@Provider
public class PersistenceExceptionHandler implements ExceptionMapper<PersistenceException> {

    @Override
    public Response toResponse(PersistenceException exception) {
        ErrorResponse error = new ErrorResponse(
                "Error de base de datos o restriccion de integridad: " + exception.getMessage(),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                LocalDateTime.now()
        );
        Log.error(exception.getMessage(), exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
    }
}