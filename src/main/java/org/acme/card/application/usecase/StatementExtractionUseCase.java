package org.acme.card.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.card.application.StatementChecks;
import org.acme.card.application.model.CardStatementView;
import org.acme.card.application.model.ExtractedCard;
import org.acme.card.application.model.ExtractedStatement;
import org.acme.card.application.model.ExtractedTransaction;
import org.acme.card.application.model.StatementConfirmation;
import org.acme.card.application.model.StatementReview;
import org.acme.card.application.port.output.CardRepository;
import org.acme.card.application.port.output.CardStatementRepository;
import org.acme.card.application.port.output.CardTransactionRepository;
import org.acme.card.domain.model.Card;
import org.acme.card.domain.model.CardStatement;
import org.acme.card.domain.model.CardStatementSource;
import org.acme.card.domain.model.CardTransaction;
import org.acme.card.domain.model.CardTransactionType;
import org.acme.card.domain.model.CategorySource;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.application.port.output.ExtractionResultRepository;
import org.acme.document.application.port.output.ProcessingJobRepository;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.document.domain.model.ProcessingStatus;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Revisar y confirmar un resumen leído por la IA (CU-015 pasos 5 y 6, CU-016). Confirmar crea el
 * resumen con sus vencimientos (como la carga manual) y sus movimientos con las cuotas.
 */
@ApplicationScoped
public class StatementExtractionUseCase {

    static final String ALREADY_REVIEWED = "Esta propuesta ya fue revisada";
    static final String NOT_A_STATEMENT = "La propuesta no es de un resumen de tarjeta";
    static final String DUE_DATE_REQUIRED = "Completá la fecha de vencimiento";
    static final String CARD_REQUIRED = "Elegí la tarjeta del resumen";

    private final ExtractionResultRepository extractions;
    private final CardRepository cards;
    private final CardStatementRepository statements;
    private final CardTransactionRepository transactions;
    private final ManageCardStatementUseCase manageStatement;
    private final DocumentRepository documents;
    private final ProcessingJobRepository jobs;
    private final ObjectMapper json;
    private final Clock clock;

    @Inject
    public StatementExtractionUseCase(ExtractionResultRepository extractions, CardRepository cards, CardStatementRepository statements,
                                      CardTransactionRepository transactions, ManageCardStatementUseCase manageStatement,
                                      DocumentRepository documents, ProcessingJobRepository jobs, ObjectMapper json, Clock clock) {
        this.extractions = extractions;
        this.cards = cards;
        this.statements = statements;
        this.transactions = transactions;
        this.manageStatement = manageStatement;
        this.documents = documents;
        this.jobs = jobs;
        this.json = json;
        this.clock = clock;
    }

    public StatementReview review(UUID userId, UUID extractionId) {
        AiExtractionResult result = find(userId, extractionId);
        ExtractedStatement statement = parse(result);
        Optional<Card> card = suggestCard(userId, statement);
        List<String> warnings = new ArrayList<>(StatementChecks.warnings(statement));
        if (card.isEmpty()) {
            warnings.add(0, "No encontramos la tarjeta de este resumen entre las tuyas: elegila o registrala.");
        } else if (statement.dueDate() != null && result.getValidationStatus() == ExtractionValidationStatus.PENDING
                && statements.findConfirmedByDueDate(userId, card.get().getId(), statement.dueDate()).isPresent()) {
            warnings.add(0, "Ya cargaste un resumen de esa tarjeta con ese vencimiento.");
        }
        UUID statementId = statements.findByExtraction(userId, extractionId).map(CardStatement::getId).orElse(null);
        return new StatementReview(result.getId(), result.getDocumentId(), result.getValidationStatus(), statement,
                card.map(Card::getId).orElse(null), warnings,
                StatementChecks.sum(statement, false), StatementChecks.sum(statement, true), statementId);
    }

    /** La última propuesta de un documento (para ir a revisarla desde el documento). */
    public Optional<UUID> latestForDocument(UUID userId, UUID documentId) {
        return extractions.findLatestForDocument(userId, documentId).map(AiExtractionResult::getId);
    }

    @Transactional
    public CardStatementView confirm(UUID userId, UUID extractionId, StatementConfirmation confirmation) {
        AiExtractionResult result = pending(userId, extractionId);
        ExtractedStatement extracted = parse(result);
        if (confirmation.cardId() == null) {
            throw new ServiceException(CARD_REQUIRED);
        }
        if (confirmation.dueDate() == null) {
            throw new ServiceException(DUE_DATE_REQUIRED);
        }

        // Mismas validaciones y vencimientos que la carga manual (tarjeta del usuario, RN-016, monedas)
        CardStatementView view = manageStatement.register(userId, CardStatement.builder()
                .cardId(confirmation.cardId())
                .documentId(result.getDocumentId())
                .extractionResultId(result.getId())
                .source(CardStatementSource.AI_EXTRACTION)
                .closingDate(confirmation.closingDate())
                .dueDate(confirmation.dueDate())
                .nextClosingDate(confirmation.nextClosingDate())
                .nextDueDate(confirmation.nextDueDate())
                .totalAmount(orZero(confirmation.totalAmount()))
                .totalAmountUsd(orZero(confirmation.totalAmountUsd()))
                .minimumPayment(confirmation.minimumPayment())
                .build());
        CardStatement statement = view.statement();
        LocalDate fallbackDate = Objects.requireNonNullElse(confirmation.closingDate(), confirmation.dueDate());
        transactions.saveAll(extracted.transactionsOrEmpty().stream()
                .map(t -> toTransaction(t, userId, statement, fallbackDate))
                .flatMap(Optional::stream)
                .toList());

        result.setValidationStatus(changed(extracted, confirmation) ? ExtractionValidationStatus.CORRECTED : ExtractionValidationStatus.CONFIRMED);
        close(userId, result, cards.findByIdForUser(userId, confirmation.cardId()).map(Card::getServiceId).orElse(null));
        return view;
    }

    /** Descartar la propuesta: no se crea nada; el documento queda guardado. */
    @Transactional
    public void reject(UUID userId, UUID extractionId) {
        AiExtractionResult result = pending(userId, extractionId);
        result.setValidationStatus(ExtractionValidationStatus.REJECTED);
        close(userId, result, null);
    }

    /** Marca la propuesta como revisada y cierra el trabajo; el documento queda asociado al servicio del banco. */
    private void close(UUID userId, AiExtractionResult result, UUID serviceId) {
        Instant now = Instant.now(clock);
        result.setReviewedAt(now);
        extractions.save(result);
        if (result.getJobId() != null) {
            jobs.findById(result.getJobId()).ifPresent(job -> {
                job.complete(now);
                jobs.save(job);
            });
        }
        documents.findByIdForUser(userId, result.getDocumentId()).ifPresent(document -> {
            document.setProcessingStatus(ProcessingStatus.COMPLETED);
            if (document.getServiceId() == null) {
                document.setServiceId(serviceId);
            }
            documents.save(document);
        });
    }

    /** La tarjeta del usuario cuyos últimos 4 (de tarjeta o de cuenta) figuran en el resumen. */
    private Optional<Card> suggestCard(UUID userId, ExtractedStatement statement) {
        Set<String> lastFours = new HashSet<>();
        if (statement.accountLastFour() != null) {
            lastFours.add(statement.accountLastFour());
        }
        statement.cardsOrEmpty().stream().map(ExtractedCard::lastFour).filter(Objects::nonNull).forEach(lastFours::add);
        return cards.findForUser(userId, true).stream()
                .filter(c -> lastFours.contains(c.getLastFour()) || lastFours.contains(c.getAccountLastFour()))
                .findFirst();
    }

    /**
     * Un movimiento de la propuesta como movimiento del resumen. Se descartan los que no tienen
     * importe; pagos y devoluciones van negativos; una cuota imposible se guarda sin cuota.
     */
    static Optional<CardTransaction> toTransaction(ExtractedTransaction t, UUID userId, CardStatement statement, LocalDate fallbackDate) {
        if (t.amount() == null || t.amount().signum() == 0) {
            return Optional.empty();
        }
        CardTransactionType type = type(t);
        BigDecimal amount = (type == CardTransactionType.PAYMENT || type == CardTransactionType.REFUND) ? t.amount().abs().negate() : t.amount();
        boolean installment = StatementChecks.validInstallment(t) && t.installmentTotal() != null;
        String merchant = t.merchant() == null || t.merchant().isBlank() ? null : cut(t.merchant().trim().toUpperCase(Locale.ROOT), 150);
        String description = t.description() == null || t.description().isBlank() ? Objects.requireNonNullElse(merchant, "Movimiento") : t.description().trim();
        return Optional.of(CardTransaction.builder()
                .userId(userId)
                .cardId(statement.getCardId())
                .statementId(statement.getId())
                .transactionDate(t.date() != null ? t.date() : fallbackDate)
                .description(cut(description, 255))
                .merchantName(merchant)
                .categorySource(CategorySource.AI)
                .transactionType(type)
                .amount(amount)
                .currency("USD".equalsIgnoreCase(t.currency()) ? "USD" : "ARS")
                .installmentNumber(installment ? t.installmentNumber() : null)
                .installmentTotal(installment ? t.installmentTotal() : null)
                .reference(t.reference() == null ? null : cut(t.reference(), 100))
                .build());
    }

    private static CardTransactionType type(ExtractedTransaction t) {
        try {
            return CardTransactionType.valueOf(t.type().trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return t.installmentTotal() != null && t.installmentTotal() > 1 ? CardTransactionType.INSTALLMENT : CardTransactionType.PURCHASE;
        }
    }

    private static boolean changed(ExtractedStatement e, StatementConfirmation c) {
        return !Objects.equals(e.dueDate(), c.dueDate()) || !Objects.equals(e.closingDate(), c.closingDate())
                || !Objects.equals(e.nextClosingDate(), c.nextClosingDate()) || !Objects.equals(e.nextDueDate(), c.nextDueDate())
                || differs(e.totalAmount(), c.totalAmount()) || differs(e.totalAmountUsd(), c.totalAmountUsd())
                || differs(e.minimumPayment(), c.minimumPayment());
    }

    private static boolean differs(BigDecimal a, BigDecimal b) {
        return orZero(a).compareTo(orZero(b)) != 0;
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String cut(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }

    private ExtractedStatement parse(AiExtractionResult result) {
        if (!ExtractedStatement.SCHEMA_VERSION.equals(result.getSchemaVersion())) {
            throw new ServiceException(NOT_A_STATEMENT);
        }
        return json.convertValue(result.getExtractedData(), ExtractedStatement.class);
    }

    private AiExtractionResult pending(UUID userId, UUID extractionId) {
        AiExtractionResult result = find(userId, extractionId);
        if (result.getValidationStatus() != ExtractionValidationStatus.PENDING) {
            throw new ServiceException(ALREADY_REVIEWED);
        }
        return result;
    }

    /** Una propuesta de otro usuario responde igual que una inexistente. */
    private AiExtractionResult find(UUID userId, UUID extractionId) {
        return extractions.findByIdForUser(userId, extractionId).orElseThrow(() -> new NotFoundException("propuesta"));
    }
}
