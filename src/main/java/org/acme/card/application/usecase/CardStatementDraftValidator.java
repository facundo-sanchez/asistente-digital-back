package org.acme.card.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.card.application.port.output.CardRepository;
import org.acme.card.application.port.output.CardStatementRepository;
import org.acme.card.domain.model.Card;
import org.acme.card.domain.model.CardStatement;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.domain.exceptions.ConflictException;
import org.acme.domain.exceptions.ServiceException;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Reglas comunes a la carga y a la corrección de un resumen. */
@ApplicationScoped
public class CardStatementDraftValidator {

    static final String UNKNOWN_CARD = "La tarjeta elegida no existe";
    static final String INACTIVE_CARD = "La tarjeta está dada de baja: reactivala para cargarle resúmenes";
    static final String UNKNOWN_DOCUMENT = "El documento elegido no existe";
    static final String INVALID_DATES = "El vencimiento no puede ser anterior al cierre";
    static final String INVALID_PERIOD = "El inicio del período no puede ser posterior al cierre";
    static final String DUPLICATE = "Ya cargaste el resumen de esta tarjeta que vence el %s";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CardRepository cards;
    private final CardStatementRepository statements;
    private final DocumentRepository documents;

    @Inject
    public CardStatementDraftValidator(CardRepository cards, CardStatementRepository statements, DocumentRepository documents) {
        this.cards = cards;
        this.statements = statements;
        this.documents = documents;
    }

    /**
     * Valida el borrador y devuelve su tarjeta. keepId: al editar, el propio resumen no cuenta
     * como repetido y la tarjeta puede estar dada de baja.
     */
    public Card validate(UUID userId, CardStatement draft, UUID keepId) {
        // Una tarjeta o documento de otro usuario responde igual que uno inexistente (RN-002)
        Card card = cards.findByIdForUser(userId, draft.getCardId()).orElseThrow(() -> new ServiceException(UNKNOWN_CARD));
        if (keepId == null && !card.isActive()) {
            throw new ServiceException(INACTIVE_CARD);
        }
        if (draft.getDocumentId() != null && documents.findByIdForUser(userId, draft.getDocumentId()).isEmpty()) {
            throw new ServiceException(UNKNOWN_DOCUMENT);
        }
        if (draft.getClosingDate() != null && draft.getDueDate().isBefore(draft.getClosingDate())) {
            throw new ServiceException(INVALID_DATES);
        }
        if (draft.getPeriodStart() != null && draft.getClosingDate() != null && draft.getPeriodStart().isAfter(draft.getClosingDate())) {
            throw new ServiceException(INVALID_PERIOD);
        }
        // RN-016: la base también lo impide (índice único); acá se responde con un mensaje claro
        statements.findConfirmedByDueDate(userId, draft.getCardId(), draft.getDueDate())
                .filter(existing -> !existing.getId().equals(keepId))
                .ifPresent(existing -> {
                    throw new ConflictException(DUPLICATE.formatted(draft.getDueDate().format(DAY)));
                });
        return card;
    }
}
