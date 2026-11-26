package org.acme.document.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.document.application.model.AiAvailability;
import org.acme.document.application.model.DocumentFile;
import org.acme.document.application.model.UploadDocumentCommand;
import org.acme.document.infrastructure.rest.ContentDisposition;
import org.acme.document.application.usecase.DocumentProcessingUseCase;
import org.acme.document.application.usecase.DocumentQueryUseCase;
import org.acme.document.application.usecase.ManageDocumentUseCase;
import org.acme.document.application.usecase.UploadDocumentUseCase;
import org.acme.document.infrastructure.rest.dto.AiAvailabilityResponse;
import org.acme.document.infrastructure.rest.dto.DocumentResponse;
import org.acme.document.infrastructure.rest.dto.ProcessingJobResponse;
import org.acme.document.infrastructure.rest.dto.DocumentUpdateRequest;
import org.acme.document.infrastructure.rest.mapper.DocumentRestMapper;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.domain.model.DocumentType;
import org.acme.shared.infrastructure.rest.EnumParams;
import org.acme.shared.infrastructure.rest.dto.PageResponse;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.UUID;

/** Repositorio documental del usuario (CU-009). El dueño sale siempre del token. */
@Path("/documents")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Documentos")
public class DocumentResource {

    @Inject
    UploadDocumentUseCase uploadDocument;

    @Inject
    DocumentQueryUseCase documentQuery;

    @Inject
    ManageDocumentUseCase manageDocument;

    @Inject
    DocumentProcessingUseCase processing;

    @Inject
    DocumentRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Listar documentos", description = "Más nuevos primero. query busca en el título y en el nombre del archivo")
    public PageResponse<DocumentResponse> list(
            @QueryParam("query") @Size(max = 100, message = "La búsqueda admite hasta 100 caracteres") String query,
            @QueryParam("documentType") @Parameter(example = "INVOICE") String documentType,
            @QueryParam("serviceId") UUID serviceId,
            @QueryParam("page") @DefaultValue("0") @Min(value = 0, message = "La página empieza en 0") int page,
            @QueryParam("size") @DefaultValue("20")
            @Min(value = 1, message = "El tamaño mínimo es 1")
            @Max(value = PageQuery.MAX_SIZE, message = "El tamaño máximo es 100") int size) {
        DocumentType type = EnumParams.parse(DocumentType.class, documentType, "Tipo de documento inválido");
        return PageResponse.from(documentQuery.list(currentUser.id(), query, type, serviceId, new PageQuery(page, size)), mapper::toResponse);
    }

    @GET
    @Path("/ai")
    @Operation(summary = "Disponibilidad de la lectura con IA", description = "false si no hay IA configurada; documentTypes: qué tipos puede leer")
    public AiAvailabilityResponse aiAvailability() {
        AiAvailability availability = processing.availability();
        return new AiAvailabilityResponse(availability.available(), availability.documentTypes());
    }

    @GET
    @Path("/{id}/processing")
    @Operation(summary = "Estado de la lectura con IA", description = "404 si nunca se pidió leerlo")
    public ProcessingJobResponse processingStatus(@PathParam("id") UUID id) {
        return processing.status(currentUser.id(), id).map(mapper::toResponse).orElseThrow(() -> new NotFoundException("lectura"));
    }

    @POST
    @Path("/{id}/process")
    @Operation(summary = "Leer el documento con IA", description = "Encola la lectura (o devuelve la que está en curso). El pedido es el consentimiento para enviarlo a la IA")
    public ProcessingJobResponse process(@PathParam("id") UUID id) {
        return mapper.toResponse(processing.request(currentUser.id(), id));
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar un documento", description = "404 si no existe, está borrado o es de otro usuario")
    public DocumentResponse get(@PathParam("id") UUID id) {
        return mapper.toResponse(documentQuery.get(currentUser.id(), id));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Editar un documento", description = "Título, tipo, servicio y vencimiento. 400 si el servicio no es del usuario")
    public DocumentResponse update(@PathParam("id") UUID id, @Valid @NotNull DocumentUpdateRequest request) {
        return mapper.toResponse(manageDocument.update(currentUser.id(), id, mapper.toCommand(request)));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Eliminar un documento", description = "Baja lógica: deja de aparecer, el archivo se conserva")
    public Response delete(@PathParam("id") UUID id) {
        manageDocument.delete(currentUser.id(), id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/download")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Descargar el archivo", description = "inline=true para mostrarlo en el navegador (vista previa)")
    public Response download(@PathParam("id") UUID id, @QueryParam("inline") boolean inline) {
        DocumentFile file = documentQuery.download(currentUser.id(), id);
        // RESTEasy envía el InputStream de a partes y lo cierra al terminar
        return Response.ok(file.content(), file.mimeType())
                .header("Content-Length", file.size())
                .header("Content-Disposition", ContentDisposition.of(inline, file.filename()))
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "private, no-store")
                .build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Subir un documento",
            description = "PDF, JPG, PNG o WEBP de hasta 10 MB. serviceId opcional (del usuario). 409 si el mismo archivo ya está subido")
    public Response upload(
            @RestForm("file") FileUpload file,
            @RestForm("documentType") String documentType,
            @RestForm("title") String title,
            @RestForm("serviceId") UUID serviceId,
            @RestForm("process") boolean process) {
        if (file == null) {
            throw new ServiceException("Elegí un archivo");
        }
        if (title != null && title.length() > 200) {
            throw new ServiceException("El título admite hasta 200 caracteres");
        }
        DocumentType type = EnumParams.parse(DocumentType.class, documentType, "Tipo de documento inválido");
        DocumentType finalType = type == null ? DocumentType.OTHER : type;
        // Antes de guardar el archivo: si se pidió leerlo con IA y no se puede, no se sube a medias
        if (process) {
            processing.checkCanProcess(currentUser.id(), finalType);
        }
        var command = new UploadDocumentCommand(file.uploadedFile(), file.fileName(), finalType, title, serviceId);
        UUID id = uploadDocument.upload(currentUser.id(), command).getId();
        if (process) {
            processing.request(currentUser.id(), id);
        }
        // Se relee para responder con el nombre del servicio asociado
        return Response.status(Response.Status.CREATED)
                .entity(mapper.toResponse(documentQuery.get(currentUser.id(), id)))
                .build();
    }
}
