package org.acme.assistant.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.assistant.infrastructure.persistence.entity.AssistantMessageEntity;

import java.util.UUID;

@ApplicationScoped
public class AssistantMessagePanacheRepository implements PanacheRepositoryBase<AssistantMessageEntity, UUID> {
}
