package org.acme.infrastructure.resources.rest.exceptions;

import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.LocalDateTime;

@Provider

public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        // Atrapa fallos de Hibernate, SQL, NullPointer, etc. sin ensuciar el repositorio
        ErrorResponse error = new ErrorResponse(
                "Ocurrio un error interno en el servidor",
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                LocalDateTime.now()
        );
        Log.error(exception.getMessage(), exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
    }
}