package org.acme.assistant.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.assistant.infrastructure.persistence.entity.ConversationEntity;

import java.util.UUID;

@ApplicationScoped
public class ConversationPanacheRepository implements PanacheRepositoryBase<ConversationEntity, UUID> {
}
