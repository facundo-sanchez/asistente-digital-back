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
        }

        if (exception instanceof NotAuthorizedException) {
            status = Response.Status.UNAUTHORIZED;
        }

        if (exception instanceof ServiceException) {
            status = Response.Status.BAD_REQUEST;
        }

        ErrorResponse error = new ErrorResponse(
                exception.getMessage(),
                status.getStatusCode(),
                LocalDateTime.now()
        );
        Log.error(exception.getMessage(), exception);
        return Response.status(status).entity(error).build();
    }
}