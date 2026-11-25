package org.acme.document.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.document.infrastructure.persistence.entity.DocumentEntity;

import java.util.UUID;

@ApplicationScoped
public class DocumentPanacheRepository implements PanacheRepositoryBase<DocumentEntity, UUID> {
}
