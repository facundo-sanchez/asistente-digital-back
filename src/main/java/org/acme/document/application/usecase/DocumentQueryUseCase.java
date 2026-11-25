package org.acme.document.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.document.application.model.DocumentFilter;
import org.acme.document.application.model.DocumentView;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.domain.model.Document;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.shared.application.SearchText;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;
import org.acme.shared.domain.model.DocumentType;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Consulta del repositorio documental del usuario, con el nombre del servicio asociado. */
@ApplicationScoped
public class DocumentQueryUseCase {

    private final DocumentRepository documents;
    private final ServiceRepository services;

    @Inject
    public DocumentQueryUseCase(DocumentRepository documents, ServiceRepository services) {
        this.documents = documents;
        this.services = services;
    }

    public PageResult<DocumentView> list(UUID userId, String text, DocumentType type, UUID serviceId, PageQuery page) {
        PageResult<Document> result = documents.findForUser(userId, new DocumentFilter(SearchText.normalize(text), type, serviceId), page);
        // Un solo viaje a la base para los nombres de los servicios de la página
        Set<UUID> serviceIds = result.content().stream().map(Document::getServiceId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> names = services.findNames(userId, serviceIds);
        // Sin servicio no se busca: los mapas inmutables (Map.of) no aceptan get(null)
        return result.map(document -> new DocumentView(document,
                document.getServiceId() == null ? null : names.get(document.getServiceId())));
    }

    /** Un documento de otro usuario o borrado responde igual que uno inexistente. */
    public DocumentView get(UUID userId, UUID documentId) {
        Document document = documents.findByIdForUser(userId, documentId).orElseThrow(() -> new NotFoundException("documento"));
        String serviceName = document.getServiceId() == null
                ? null
                : services.findNames(userId, Set.of(document.getServiceId())).get(document.getServiceId());
        return new DocumentView(document, serviceName);
    }
}
