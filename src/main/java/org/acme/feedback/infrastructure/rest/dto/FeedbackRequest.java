package org.acme.feedback.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import org.acme.feedback.domain.model.FeedbackKind;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Comentario, sugerencia o reporte de error. details: datos técnicos del error (JSON como texto). */
@Schema(name = "FeedbackRequest")
public record FeedbackRequest(@NotNull(message = "Elegí qué querés mandar") FeedbackKind kind,
                              @Schema(example = "Estaría bueno ver las facturas por año") String message,
                              @Schema(example = "/invoices") String page,
                              String details) {
}
