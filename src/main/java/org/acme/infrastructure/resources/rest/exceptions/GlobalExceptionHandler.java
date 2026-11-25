package org.acme.infrastructure.resources.rest.exceptions;

import io.quarkus.logging.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.LocalDateTime;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        // Errores HTTP de JAX-RS (JSON mal formado, ruta inexistente, método no permitido):
        // se respeta su status en lugar de convertirlos en 500.
        if (exception instanceof WebApplicationException wae && wae.getResponse() != null) {
            int status = wae.getResponse().getStatus();
            if (status < 500) {
                ErrorResponse error = new ErrorResponse(
                        status == 400 ? "La solicitud no es válida" : Response.Status.fromStatusCode(status) != null
                                ? Response.Status.fromStatusCode(status).getReasonPhrase() : "Error en la solicitud",
                        status,
                        LocalDateTime.now()
                );
                return Response.status(status).type(MediaType.APPLICATION_JSON).entity(error).build();
            }
        }

        // Cualquier otro fallo: detalle solo en el log
        Log.error(exception.getMessage(), exception);
        ErrorResponse error = new ErrorResponse(
                "Ocurrió un error interno en el servidor",
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                LocalDateTime.now()
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(error).build();
    }
}
