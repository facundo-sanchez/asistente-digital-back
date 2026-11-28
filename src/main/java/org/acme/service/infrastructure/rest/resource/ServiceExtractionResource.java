package org.acme.service.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.provider.infrastructure.rest.mapper.ProviderRestMapper;
import org.acme.service.application.model.ServiceProofReview;
import org.acme.service.application.usecase.ServiceExtractionUseCase;
import org.acme.service.infrastructure.rest.dto.ServiceProofReviewResponse;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/** Revisión de un comprobante de servicio leído con IA; el alta es POST /services?extractionId=. */
@Path("/service-extractions")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Servicios")
public class ServiceExtractionResource {

    @Inject
    ServiceExtractionUseCase extraction;

    @Inject
    ProviderRestMapper providers;

    @Inject
    CurrentUser currentUser;

    @GET
    @Path("/{id}")
    @Operation(summary = "Comprobante de servicio leído", description = "Proveedor, nombre, categoría y nro. de cliente para completar el alta")
    public ServiceProofReviewResponse review(@PathParam("id") UUID id) {
        ServiceProofReview r = extraction.review(currentUser.id(), id);
        return new ServiceProofReviewResponse(r.extractionId(), r.documentId(), r.status(), r.proof(),
                r.provider() == null ? null : providers.toResponse(r.provider()), r.warnings());
    }
}
