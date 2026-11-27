package org.acme.loan.application.usecase;

import org.acme.document.application.usecase.InMemoryDocumentRepository;
import org.acme.loan.application.model.LoanView;
import org.acme.loan.application.model.NewLoan;
import org.acme.loan.domain.model.Loan;
import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.payment.application.usecase.InMemoryPaymentRepository;
import org.acme.payment.application.usecase.PaymentDraftValidator;
import org.acme.payment.application.usecase.RegisterPaymentUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoanNoticeUseCaseTest {

    private final InMemoryObligationRepository obligations = new InMemoryObligationRepository();
    private final InMemoryLoanRepository loanRows = new InMemoryLoanRepository();
    private LoanUseCase loans;
    private LoanNoticeUseCase notices;
    private final UUID ana = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        RegisterPaymentUseCase register = new RegisterPaymentUseCase(new InMemoryPaymentRepository(obligations), obligations,
                new PaymentDraftValidator(new InMemoryDocumentRepository()));
        loans = new LoanUseCase(loanRows, obligations, register, Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC));
        notices = new LoanNoticeUseCase(loanRows, obligations);
    }

    @Test
    void conUnSoloPrestamoDelBancoLoAsociaYPoneElImporteReal() {
        UUID id = create("BBVA", 9);

        assertNull(notices.apply(ana, "BBVA", 10, "203/2", new BigDecimal("584292.14"), LocalDate.of(2026, 11, 7)));

        LoanView view = loans.get(ana, id);
        assertEquals("203/2", view.loan().getShortReference());
        var tenth = view.installments().get(9).obligation();
        assertEquals(0, new BigDecimal("584292.14").compareTo(tenth.getAmount()));
        assertFalse(tenth.isEstimated());
    }

    @Test
    void conVariosDelMismoBancoNoAdivinaYNoTocaCuotasPagadas() {
        create("BBVA", 9);
        create("BBVA Francés", 0);
        assertEquals(LoanNoticeUseCase.AMBIGUOUS, notices.apply(ana, "BBVA", 10, "203/2", BigDecimal.TEN, LocalDate.of(2026, 11, 7)));
        assertEquals(LoanNoticeUseCase.NOT_FOUND, notices.apply(ana, "Galicia", 10, "1/1", BigDecimal.TEN, LocalDate.of(2026, 11, 7)));
    }

    @Test
    void unaCuotaYaPagadaNoCambia() {
        create("BBVA", 10);
        assertEquals(LoanNoticeUseCase.PAID, notices.apply(ana, "BBVA", 10, "203/2", BigDecimal.TEN, LocalDate.of(2026, 11, 7)));
    }

    private UUID create(String lender, int paid) {
        Loan loan = Loan.builder().lender(lender).name("Préstamo personal").currency("ARS").installmentsCount(30)
                .firstDueDate(LocalDate.of(2026, 2, 7)).build();
        return loans.create(ana, new NewLoan(loan, new BigDecimal("584292.14"), paid)).loan().getId();
    }
}
