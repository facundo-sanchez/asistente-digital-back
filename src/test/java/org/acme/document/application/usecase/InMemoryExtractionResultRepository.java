package org.acme.document.application.usecase;

import org.acme.document.application.port.output.ExtractionResultRepository;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.ExtractionValidationStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Propuestas de la IA en memoria. */
public class InMemoryExtractionResultRepository implements ExtractionResultRepository {

    public final Map<UUID, AiExtractionResult> rows = new LinkedHashMap<>();
    private Instant createdAt = Instant.parse("2026-11-26T10:00:00Z");

    @Override
    public AiExtractionResult save(AiExtractionResult result) {
        AiExtractionResult copy = result.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
            copy.setCreatedAt(createdAt);
            createdAt = createdAt.plusSeconds(1);
        }
        rows.put(copy.getId(), copy);
        return copy.toBuilder().build();
    }

    @Override
    public Optional<AiExtractionResult> findByIdForUser(UUID userId, UUID resultId) {
        return Optional.ofNullable(rows.get(resultId)).filter(r -> r.getUserId().equals(userId)).map(r -> r.toBuilder().build());
    }

    @Override
    public Optional<AiExtractionResult> findLatestForDocument(UUID userId, UUID documentId) {
        return rows.values().stream().filter(r -> r.getUserId().equals(userId) && r.getDocumentId().equals(documentId))
                .max(Comparator.comparing(AiExtractionResult::getCreatedAt)).map(r -> r.toBuilder().build());
    }

    @Override
    public List<AiExtractionResult> findPending(UUID userId) {
        return rows.values().stream().filter(r -> r.getUserId().equals(userId) && r.getValidationStatus() == ExtractionValidationStatus.PENDING)
                .sorted(Comparator.comparing(AiExtractionResult::getCreatedAt)).toList();
    }
}
