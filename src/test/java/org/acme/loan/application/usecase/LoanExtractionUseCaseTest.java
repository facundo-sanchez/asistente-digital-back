package org.acme.loan.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.acme.document.application.usecase.ExtractionReviews;
import org.acme.document.application.usecase.InMemoryDocumentRepository;
import org.acme.document.application.usecase.InMemoryExtractionResultRepository;
import org.acme.document.application.usecase.InMemoryProcessingJobRepository;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.domain.exceptions.ServiceException;
import org.acme.loan.application.model.*;
import org.acme.loan.domain.model.Loan;
import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.payment.application.usecase.InMemoryPaymentRepository;
import org.acme.payment.application.usecase.PaymentDraftValidator;
import org.acme.payment.application.usecase.RegisterPaymentUseCase;
import org.acme.shared.domain.model.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoanExtractionUseCaseTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-27T12:00:00Z"), ZoneOffset.UTC);
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final InMemoryObligationRepository obligations = new InMemoryObligationRepository();
    private final InMemoryPaymentRepository paymentRows = new InMemoryPaymentRepository(obligations);
    private final InMemoryExtractionResultRepository extractions = new InMemoryExtractionResultRepository();
    private final InMemoryLoanRepository loanRows = new InMemoryLoanRepository();
    private LoanExtractionUseCase useCase;
    private LoanUseCase loans;
    private final UUID ana = UUID.randomUUID();

    private final InMemoryDocumentRepository documents = new InMemoryDocumentRepository();

    @BeforeEach
    void setUp() {
        RegisterPaymentUseCase register = new RegisterPaymentUseCase(paymentRows, obligations, new PaymentDraftValidator(documents));
        loans = new LoanUseCase(loanRows, obligations, register, clock);
        ExtractionReviews reviews = new ExtractionReviews(extractions, documents, new InMemoryProcessingJobRepository(), clock);
        useCase = new LoanExtractionUseCase(reviews, loanRows, obligations, register, loans, json);
    }

    @Test
    void elComprobanteDePagoEncuentraElPrestamoYRegistraLaCuotaConSuImporteYSaldo() {
        UUID loanId = loans.create(ana, bbva(9)).loan().getId();
        UUID extraction = receipt(10, 20, "584292.14", "5820337.83");

        LoanReceiptReview review = useCase.receiptReview(ana, extraction);
        assertEquals(loanId, review.suggestedLoanId());
        assertTrue(review.warnings().isEmpty(), review.warnings().toString());

        LoanView view = useCase.confirmReceipt(ana, extraction,
                new LoanReceiptConfirmation(loanId, 10, new BigDecimal("584292.14"), LocalDate.of(2026, 11, 11), new BigDecimal("5820337.83")));

        var tenth = view.installments().get(9);
        assertEquals(EffectiveObligationStatus.PAID, tenth.effectiveStatus());
        assertFalse(tenth.obligation().isEstimated());
        assertEquals(10, view.paidCount());
        assertEquals(0, new BigDecimal("5820337.83").compareTo(view.loan().getBankBalance()));
        var payment = paymentRows.findForObligation(ana, tenth.obligation().getId()).getFirst();
        assertEquals("Capital 150.728,02 · Intereses 353.288,06", payment.getNotes());
        assertNotNull(payment.getReceiptDocumentId());
        assertThrows(ServiceException.class, () -> useCase.confirmReceipt(ana, extraction,
                new LoanReceiptConfirmation(loanId, 10, BigDecimal.ONE, LocalDate.now(), null)), "la propuesta ya se revisó");
    }

    @Test
    void avisaSiNoEncuentraElPrestamoOLaCuotaYaEstaPaga() {
        assertFalse(useCase.receiptReview(ana, receipt(10, 20, "1", null)).warnings().isEmpty());

        loans.create(ana, bbva(10));
        LoanReceiptReview review = useCase.receiptReview(ana, receipt(10, 20, "1", null));
        assertTrue(review.warnings().stream().anyMatch(w -> w.contains("ya figura pagada")));
    }

    @Test
    void altaDesdeElComprobanteDejaElDocumentoComoContrato() {
        UUID extraction = save("loan_contract/1", DocumentType.LOAN_CONTRACT, Map.of("lender", "BBVA", "loanNumber", "00170280009600182032"));

        LoanView view = useCase.createFromContract(ana, extraction, bbva(0));

        assertNotNull(view.loan().getContractDocumentId());
        assertEquals(ExtractionValidationStatus.CONFIRMED, extractions.findByIdForUser(ana, extraction).orElseThrow().getValidationStatus());
    }

    private UUID receipt(int number, int pending, String amount, String newBalance) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("loanNumber", "00170280009600182032");
        data.put("installmentNumber", number);
        data.put("pendingInstallments", pending);
        data.put("totalAmount", new BigDecimal(amount));
        data.put("capital", new BigDecimal("150728.02"));
        data.put("interest", new BigDecimal("353288.06"));
        if (newBalance != null) data.put("newBalance", new BigDecimal(newBalance));
        return save("loan_receipt/1", DocumentType.LOAN_RECEIPT, data);
    }

    private UUID save(String schema, DocumentType type, Map<String, Object> data) {
        UUID document = documents.save(org.acme.document.domain.model.Document.builder().userId(ana).documentType(type).originalFilename("comprobante.pdf").build()).getId();
        return extractions.save(AiExtractionResult.builder().userId(ana).documentId(document).schemaVersion(schema).detectedType(type)
                .extractedData(new LinkedHashMap<>(data)).validationStatus(ExtractionValidationStatus.PENDING).build()).getId();
    }

    private static NewLoan bbva(int paid) {
        Loan loan = Loan.builder().lender("BBVA").name("Préstamo personal").loanNumber("00170280009600182032").currency("ARS")
                .installmentsCount(30).firstDueDate(LocalDate.of(2026, 2, 7)).build();
        return new NewLoan(loan, new BigDecimal("584292.14"), paid);
    }
}
