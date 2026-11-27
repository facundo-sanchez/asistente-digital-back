package org.acme.loan.application.usecase;

import org.acme.document.application.usecase.InMemoryDocumentRepository;
import org.acme.domain.exceptions.ConflictException;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.loan.application.model.LoanView;
import org.acme.loan.application.model.NewLoan;
import org.acme.loan.domain.model.Loan;
import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.obligation.domain.model.ObligationType;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoanUseCaseTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-27T12:00:00Z"), ZoneOffset.UTC);
    private final InMemoryObligationRepository obligations = new InMemoryObligationRepository();
    private LoanUseCase loans;
    private final UUID ana = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        InMemoryPaymentRepository payments = new InMemoryPaymentRepository(obligations);
        RegisterPaymentUseCase register = new RegisterPaymentUseCase(payments, obligations, new PaymentDraftValidator(new InMemoryDocumentRepository()));
        loans = new LoanUseCase(new InMemoryLoanRepository(), obligations, register, clock);
    }

    @Test
    void lasCuotasCaenElMismoDiaYSiElMesNoLoTieneElUltimo() {
        assertEquals(List.of(LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31)),
                LoanUseCase.installmentDates(LocalDate.of(2027, 1, 31), 3));
    }

    @Test
    void creaLasCuotasComoVencimientosYLasYaPagadasQuedanPagadas() {
        LoanView view = loans.create(ana, bbva(30, 9));

        assertEquals(30, view.installments().size());
        var tenth = view.installments().get(9).obligation();
        assertEquals(ObligationType.LOAN, tenth.getObligationType());
        assertEquals(10, tenth.getInstallmentNumber());
        assertEquals(LocalDate.of(2026, 11, 7), tenth.getDueDate());
        assertEquals("Préstamo Préstamo personal · cuota 10/30", tenth.getTitle());
        assertTrue(tenth.isEstimated());
        assertEquals(9, view.paidCount());
        assertEquals(EffectiveObligationStatus.PAID, view.installments().get(8).effectiveStatus());
        assertEquals(0, new BigDecimal("584292.14").multiply(BigDecimal.valueOf(21)).compareTo(view.remaining()));
        assertEquals(10, view.next().orElseThrow().obligation().getInstallmentNumber());
    }

    @Test
    void validaCuotasYaPagadasYNumeroRepetido() {
        assertThrows(ServiceException.class, () -> loans.create(ana, bbva(12, 12)));
        loans.create(ana, bbva(30, 0));
        assertThrows(ConflictException.class, () -> loans.create(ana, bbva(30, 0)));
    }

    @Test
    void cambiaElImporteDeUnaCuotaPendientePeroNoDeUnaPagada() {
        LoanView view = loans.create(ana, bbva(30, 9));
        UUID id = view.loan().getId();

        LoanView changed = loans.changeInstallment(ana, id, 10, new BigDecimal("590000"));
        var tenth = changed.installments().get(9).obligation();
        assertEquals(0, new BigDecimal("590000").compareTo(tenth.getAmount()));
        assertFalse(tenth.isEstimated());

        assertThrows(ServiceException.class, () -> loans.changeInstallment(ana, id, 3, new BigDecimal("1")));
    }

    @Test
    void cancelarDejaLasPagadasYCancelaLasPendientes() {
        LoanView view = loans.create(ana, bbva(30, 9));

        LoanView cancelled = loans.cancel(ana, view.loan().getId(), "Cancelación anticipada");

        assertEquals("CANCELLED", cancelled.effectiveStatus());
        assertEquals(9, cancelled.paidCount());
        assertTrue(cancelled.installments().subList(9, 30).stream().allMatch(i -> i.obligation().getStatus() == ObligationStatus.CANCELLED));
    }

    @Test
    void otroUsuarioNoVeElPrestamo() {
        UUID id = loans.create(ana, bbva(30, 0)).loan().getId();

        assertThrows(NotFoundException.class, () -> loans.get(UUID.randomUUID(), id));
        assertTrue(loans.list(UUID.randomUUID()).isEmpty());
    }

    private static NewLoan bbva(int count, int paid) {
        Loan loan = Loan.builder().lender("BBVA").name("Préstamo personal").loanNumber("00170280009600182032").currency("ARS")
                .principal(new BigDecimal("7000000")).installmentsCount(count).firstDueDate(LocalDate.of(2026, 2, 7)).build();
        return new NewLoan(loan, new BigDecimal("584292.14"), paid);
    }
}
