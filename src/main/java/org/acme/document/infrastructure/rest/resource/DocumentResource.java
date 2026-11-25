package org.acme.document.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.document.application.model.DocumentFile;
import org.acme.document.application.model.UploadDocumentCommand;
import org.acme.document.infrastructure.rest.ContentDisposition;
import org.acme.document.application.usecase.DocumentQueryUseCase;
import org.acme.document.application.usecase.UploadDocumentUseCase;
import org.acme.document.infrastructure.rest.dto.DocumentResponse;
import org.acme.document.infrastructure.rest.mapper.DocumentRestMapper;
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
    @Path("/{id}")
    @Operation(summary = "Consultar un documento", description = "404 si no existe, está borrado o es de otro usuario")
    public DocumentResponse get(@PathParam("id") UUID id) {
        return mapper.toResponse(documentQuery.get(currentUser.id(), id));
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
            description = "PDF, JPG, PNG o WEBP de hasta 10 MB. 409 si el mismo archivo ya está subido")
    public Response upload(
            @RestForm("file") FileUpload file,
            @RestForm("documentType") String documentType,
            @RestForm("title") String title) {
        if (file == null) {
            throw new ServiceException("Elegí un archivo");
        }
        if (title != null && title.length() > 200) {
            throw new ServiceException("El título admite hasta 200 caracteres");
        }
        DocumentType type = EnumParams.parse(DocumentType.class, documentType, "Tipo de documento inválido");
        var command = new UploadDocumentCommand(file.uploadedFile(), file.fileName(), type == null ? DocumentType.OTHER : type, title);
        return Response.status(Response.Status.CREATED)
                .entity(mapper.toResponse(uploadDocument.upload(currentUser.id(), command), null))
                .build();
    }
}
