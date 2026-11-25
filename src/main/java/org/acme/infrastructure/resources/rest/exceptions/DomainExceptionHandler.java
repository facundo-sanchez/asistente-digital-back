package org.acme.infrastructure.resources.rest.exceptions;

import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.domain.exceptions.*;

import java.time.LocalDateTime;

@Provider
public class DomainExceptionHandler implements ExceptionMapper<DomainException> {

    @Override
    public Response toResponse(DomainException exception) {
        Response.Status status = Response.Status.BAD_REQUEST;

        if (exception instanceof NotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (exception instanceof NotAuthorizedException) {
            status = Response.Status.UNAUTHORIZED;
        } else if (exception instanceof ConflictException) {
            status = Response.Status.CONFLICT;
        } else if (exception instanceof UnavailableException) {
            status = Response.Status.SERVICE_UNAVAILABLE;
        }

        ErrorResponse error = new ErrorResponse(
                exception.getMessage(),
                status.getStatusCode(),
                LocalDateTime.now()
        );
        // Errores esperables de negocio: sin stack trace en el log
        Log.debugf("%s: %s", exception.getClass().getSimpleName(), exception.getMessage());
        return Response.status(status).entity(error).build();
    }
}
