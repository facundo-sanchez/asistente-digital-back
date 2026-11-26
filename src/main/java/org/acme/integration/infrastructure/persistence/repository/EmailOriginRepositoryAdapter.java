package org.acme.integration.infrastructure.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.integration.application.model.EmailOrigin;
import org.acme.integration.application.port.output.EmailOriginRepository;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EmailOriginRepositoryAdapter implements EmailOriginRepository {

    @Inject
    ProcessedItemPanacheRepository items;

    @Override
    public Optional<EmailOrigin> findByDocument(UUID documentId) {
        return items.find("documentId", documentId).firstResultOptional().map(i -> new EmailOrigin(i.getProviderId(), i.getServiceId()));
    }
}
