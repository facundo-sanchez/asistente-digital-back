package org.acme.card.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.card.application.model.CardStatementView;
import org.acme.card.application.port.output.CardRepository;
import org.acme.card.application.port.output.CardStatementRepository;
import org.acme.card.domain.model.Card;
import org.acme.card.domain.model.CardStatement;
import org.acme.card.domain.model.CardStatementSource;
import org.acme.card.domain.model.CardStatementStatus;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resúmenes de tarjeta (CU-015): carga manual (queda confirmado y genera sus vencimientos),
 * consulta, corrección y anulación. Cierre y vencimiento son los que informa el banco.
 */
@ApplicationScoped
public class ManageCardStatementUseCase {

    static final String NOT_EDITABLE = "Un resumen anulado no se puede editar";

    private final CardStatementRepository statements;
    private final CardRepository cards;
    private final CardStatementDraftValidator validator;
    private final StatementObligations obligations;
    private final Clock clock;

    @Inject
    public ManageCardStatementUseCase(CardStatementRepository statements, CardRepository cards, CardStatementDraftValidator validator,
                                      StatementObligations obligations, Clock clock) {
        this.statements = statements;
        this.cards = cards;
        this.validator = validator;
        this.obligations = obligations;
        this.clock = clock;
    }

    @Transactional
    public CardStatementView register(UUID userId, CardStatement draft) {
        Card card = validator.validate(userId, draft, null);
        Instant now = Instant.now(clock);
        CardStatement statement = draft.toBuilder()
                .id(null)
                .userId(userId)
                .currency(card.getCurrency())
                // Manual salvo que venga de una propuesta de la IA (con su extractionResultId)
                .source(draft.getSource() == null ? CardStatementSource.MANUAL : draft.getSource())
                .status(CardStatementStatus.PENDING_VALIDATION)
                .obligationId(null)
                .obligationUsdId(null)
                .build();
        statement.confirm(now);
        // Se guarda primero (para tener id) y otra vez con los ids de sus vencimientos
        statement = statements.save(statement);
        obligations.sync(userId, card, statement, now);
        return view(userId, statements.save(statement), card);
    }

    @Transactional
    public CardStatementView update(UUID userId, UUID statementId, CardStatement draft) {
        CardStatement statement = find(userId, statementId);
        if (statement.getStatus() == CardStatementStatus.REJECTED) {
            throw new ServiceException(NOT_EDITABLE);
        }
        obligations.requireNoPayments(userId, statement);
        // La tarjeta de un resumen no cambia
        CardStatement changes = draft.toBuilder().cardId(statement.getCardId()).build();
        Card card = validator.validate(userId, changes, statement.getId());

        statement.setDocumentId(changes.getDocumentId());
        statement.setPeriodStart(changes.getPeriodStart());
        statement.setClosingDate(changes.getClosingDate());
        statement.setDueDate(changes.getDueDate());
        statement.setNextClosingDate(changes.getNextClosingDate());
        statement.setNextDueDate(changes.getNextDueDate());
        statement.setTotalAmount(changes.getTotalAmount());
        statement.setTotalAmountUsd(changes.getTotalAmountUsd());
        statement.setMinimumPayment(changes.getMinimumPayment());
        obligations.sync(userId, card, statement, Instant.now(clock));
        return view(userId, statements.save(statement), card);
    }

    /** Anula el resumen y cancela sus vencimientos. Queda en el historial. */
    @Transactional
    public void reject(UUID userId, UUID statementId) {
        CardStatement statement = find(userId, statementId);
        obligations.requireNoPayments(userId, statement);
        statement.reject();
        obligations.cancelAll(userId, statement, Instant.now(clock));
        statements.save(statement);
    }

    public CardStatementView get(UUID userId, UUID statementId) {
        CardStatement statement = find(userId, statementId);
        return view(userId, statement, cards.findByIdForUser(userId, statement.getCardId()).orElseThrow());
    }

    /** Los de una tarjeta del usuario, más nuevos primero. */
    public List<CardStatementView> listForCard(UUID userId, UUID cardId) {
        Card card = cards.findByIdForUser(userId, cardId).orElseThrow(() -> new NotFoundException("tarjeta"));
        return statements.findForCard(userId, cardId).stream().map(s -> view(userId, s, card)).toList();
    }

    /** El resumen que generó ese vencimiento, si lo hay. */
    public Optional<CardStatementView> findByObligation(UUID userId, UUID obligationId) {
        return statements.findByObligation(userId, obligationId).map(s -> get(userId, s.getId()));
    }

    // shortcut: dos consultas de vencimiento por resumen; con listas largas, traerlos juntos (findByIds)
    private CardStatementView view(UUID userId, CardStatement statement, Card card) {
        return new CardStatementView(statement, card.getName(),
                obligations.find(userId, statement.getObligationId()), obligations.find(userId, statement.getObligationUsdId()));
    }

    /** Un resumen de otro usuario responde igual que uno inexistente. */
    private CardStatement find(UUID userId, UUID statementId) {
        return statements.findByIdForUser(userId, statementId).orElseThrow(() -> new NotFoundException("resumen"));
    }
}
