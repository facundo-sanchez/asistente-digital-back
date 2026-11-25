package org.acme.infrastructure.resources.rest.exceptions;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** 400 con el detalle por campo de Bean Validation, en el mismo formato que el resto de los errores. */
@Provider
public class ValidationExceptionHandler implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(v -> new FieldViolation(fieldName(v), v.getMessage()))
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();
        ErrorResponse error = new ErrorResponse(
                "Los datos enviados no son válidos",
                Response.Status.BAD_REQUEST.getStatusCode(),
                LocalDateTime.now(),
                violations
        );
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }

    /** "register.request.email" -> "email". Para validaciones de clase (@AssertTrue) queda el nombre del método. */
    private static String fieldName(ConstraintViolation<?> violation) {
        String name = null;
        for (Path.Node node : violation.getPropertyPath()) {
            name = node.getName();
        }
        return name == null ? "" : name;
    }
}
