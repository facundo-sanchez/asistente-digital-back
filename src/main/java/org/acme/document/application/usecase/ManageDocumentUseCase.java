package org.acme.document.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.document.application.model.DocumentView;
import org.acme.document.application.model.UpdateDocumentCommand;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.domain.model.Document;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.service.domain.model.Service;

import java.time.Instant;
import java.util.UUID;

/** Editar los datos de un documento y darlo de baja. El archivo no cambia. */
@ApplicationScoped
public class ManageDocumentUseCase {

    static final String UNKNOWN_SERVICE = "El servicio elegido no existe";

    private final DocumentRepository documents;
    private final ServiceRepository services;

    @Inject
    public ManageDocumentUseCase(DocumentRepository documents, ServiceRepository services) {
        this.documents = documents;
        this.services = services;
    }

    @Transactional
    public DocumentView update(UUID userId, UUID documentId, UpdateDocumentCommand command) {
        Document document = find(userId, documentId);
        // Un servicio de otro usuario responde igual que uno inexistente
        Service service = command.serviceId() == null
                ? null
                : services.findByIdForUser(userId, command.serviceId()).orElseThrow(() -> new ServiceException(UNKNOWN_SERVICE));

        document.setTitle(command.title() == null || command.title().isBlank() ? null : command.title().trim());
        document.setDocumentType(command.documentType());
        document.setServiceId(command.serviceId());
        document.setExpiresOn(command.expiresOn());
        return new DocumentView(documents.save(document), service == null ? null : service.getName());
    }

    /** Baja lógica: el archivo se conserva y el documento deja de aparecer. */
    @Transactional
    public void delete(UUID userId, UUID documentId) {
        Document document = find(userId, documentId);
        document.delete(Instant.now());
        documents.save(document);
    }

    private Document find(UUID userId, UUID documentId) {
        return documents.findByIdForUser(userId, documentId).orElseThrow(() -> new NotFoundException("documento"));
    }
}
