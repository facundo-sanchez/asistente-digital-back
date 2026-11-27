package org.acme.loan.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.ConflictException;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.loan.application.model.LoanView;
import org.acme.loan.application.model.NewLoan;
import org.acme.loan.application.port.output.LoanRepository;
import org.acme.loan.domain.model.Loan;
import org.acme.loan.domain.model.LoanStatus;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.obligation.domain.model.ObligationType;
import org.acme.payment.application.usecase.RegisterPaymentUseCase;
import org.acme.payment.domain.model.Payment;
import org.acme.payment.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Préstamos tomados (módulo 13): alta con sus cuotas como vencimientos, ficha, cambiar el importe de una cuota y cancelar. */
@ApplicationScoped
public class LoanUseCase {

    static final Set<String> CURRENCIES = Set.of("ARS", "USD");
    static final int MAX_INSTALLMENTS = 360;
    static final String PAID_BEFORE = "Cuota pagada antes de cargar el préstamo";

    private final LoanRepository loans;
    private final ObligationRepository obligations;
    private final RegisterPaymentUseCase payments;
    private final Clock clock;

    @Inject
    public LoanUseCase(LoanRepository loans, ObligationRepository obligations, RegisterPaymentUseCase payments, Clock clock) {
        this.loans = loans;
        this.obligations = obligations;
        this.payments = payments;
        this.clock = clock;
    }

    /** Crea el préstamo y sus N cuotas; las que ya se pagaron quedan pagadas (sin comprobante). */
    @Transactional
    public LoanView create(UUID userId, NewLoan request) {
        Loan draft = clean(request.loan());
        validate(userId, draft, null);
        if (request.installmentAmount() == null || request.installmentAmount().signum() <= 0) {
            throw new ServiceException("Poné el importe de la cuota");
        }
        if (request.paidInstallments() < 0 || request.paidInstallments() >= draft.getInstallmentsCount()) {
            throw new ServiceException("Las cuotas ya pagadas tienen que ser menos que el total");
        }
        Loan loan = loans.save(draft.toBuilder().id(null).userId(userId).status(LoanStatus.ACTIVE).build());
        List<LocalDate> dates = installmentDates(loan.getFirstDueDate(), loan.getInstallmentsCount());
        LocalDate today = LocalDate.now(clock);
        for (int n = 1; n <= dates.size(); n++) {
            ObligationSummary installment = obligations.save(Obligation.builder()
                    .userId(userId)
                    .loanId(loan.getId())
                    .installmentNumber(n)
                    .title(title(loan, n))
                    .obligationType(ObligationType.LOAN)
                    .amount(request.installmentAmount())
                    .currency(loan.getCurrency())
                    .dueDate(dates.get(n - 1))
                    // El importe real lo pone el comprobante o el aviso del banco
                    .estimated(true)
                    .build());
            if (n <= request.paidInstallments()) {
                LocalDate due = installment.obligation().getDueDate();
                payments.register(userId, Payment.builder()
                        .obligationId(installment.obligation().getId())
                        .amount(request.installmentAmount())
                        .currency(loan.getCurrency())
                        .paymentDate(due.isAfter(today) ? today : due)
                        .paymentMethod(PaymentMethod.OTHER)
                        .notes(PAID_BEFORE)
                        .build());
            }
        }
        return view(userId, loan);
    }

    // shortcut: una consulta de cuotas por préstamo; con muchos préstamos por usuario, traerlas en una sola
    public List<LoanView> list(UUID userId) {
        return loans.findForUser(userId).stream().map(l -> view(userId, l)).toList();
    }

    public LoanView get(UUID userId, UUID loanId) {
        return view(userId, find(userId, loanId));
    }

    /** Datos que no cambian el cronograma: entidad, nombre, número, referencia, tasa, contrato y notas. */
    @Transactional
    public LoanView update(UUID userId, UUID loanId, Loan changes) {
        Loan loan = find(userId, loanId);
        Loan clean = clean(changes);
        if (clean.getLoanNumber() != null && loans.existsNumber(userId, clean.getLoanNumber(), loanId)) {
            throw new ConflictException("Ya tenés otro préstamo con ese número");
        }
        loan.setLender(clean.getLender());
        loan.setName(clean.getName());
        loan.setLoanNumber(clean.getLoanNumber());
        loan.setShortReference(clean.getShortReference());
        loan.setRateType(clean.getRateType());
        loan.setAnnualRate(clean.getAnnualRate());
        loan.setPrincipal(clean.getPrincipal());
        loan.setDisbursementDate(clean.getDisbursementDate());
        loan.setContractDocumentId(clean.getContractDocumentId());
        loan.setNotes(clean.getNotes());
        return view(userId, loans.save(loan));
    }

    /** Cambia el importe de una cuota pendiente sin pagos (ej. el banco avisó el importe real). */
    @Transactional
    public LoanView changeInstallment(UUID userId, UUID loanId, int number, BigDecimal amount) {
        Loan loan = find(userId, loanId);
        if (amount == null || amount.signum() <= 0) {
            throw new ServiceException("Poné un importe mayor a cero");
        }
        ObligationSummary installment = obligations.findByLoan(userId, loan.getId()).stream()
                .filter(i -> i.obligation().getInstallmentNumber() == number).findFirst()
                .orElseThrow(() -> new NotFoundException("cuota"));
        if (installment.paidAmount().signum() > 0 || installment.obligation().getStatus() != org.acme.obligation.domain.model.ObligationStatus.PENDING) {
            throw new ServiceException("Esa cuota ya tiene pagos o está cancelada: no se puede cambiar el importe");
        }
        Obligation obligation = installment.obligation();
        obligation.setAmount(amount);
        obligation.setEstimated(false);
        obligations.save(obligation);
        return view(userId, loan);
    }

    /** Cancela el préstamo: las cuotas pendientes sin pagos se cancelan; las pagadas quedan. */
    @Transactional
    public LoanView cancel(UUID userId, UUID loanId, String reason) {
        Loan loan = find(userId, loanId);
        if (loan.getStatus() == LoanStatus.CANCELLED) {
            throw new ServiceException("El préstamo ya está cancelado");
        }
        String clean = reason == null || reason.isBlank() ? "Préstamo cancelado" : reason.trim();
        Instant now = Instant.now(clock);
        for (ObligationSummary installment : obligations.findByLoan(userId, loanId)) {
            Obligation o = installment.obligation();
            if (o.getStatus() == org.acme.obligation.domain.model.ObligationStatus.PENDING && installment.paidAmount().signum() == 0) {
                o.cancel(clean, now);
                obligations.save(o);
            }
        }
        loan.setStatus(LoanStatus.CANCELLED);
        loan.setCancelReason(clean);
        return view(userId, loans.save(loan));
    }

    /** El mismo día de cada mes que la primera; si un mes no lo tiene, el último (31/1 → 28/2 → 31/3). */
    static List<LocalDate> installmentDates(LocalDate first, int count) {
        List<LocalDate> dates = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            dates.add(first.plusMonths(i));
        }
        return dates;
    }

    static String title(Loan loan, int number) {
        return "Préstamo " + loan.getName() + " · cuota " + number + "/" + loan.getInstallmentsCount();
    }

    private void validate(UUID userId, Loan loan, UUID excludeId) {
        if (loan.getLender() == null || loan.getName() == null) {
            throw new ServiceException("Poné la entidad y un nombre para el préstamo");
        }
        if (!CURRENCIES.contains(loan.getCurrency())) {
            throw new ServiceException("La moneda tiene que ser ARS o USD");
        }
        if (loan.getInstallmentsCount() < 1 || loan.getInstallmentsCount() > MAX_INSTALLMENTS) {
            throw new ServiceException("La cantidad de cuotas va de 1 a " + MAX_INSTALLMENTS);
        }
        if (loan.getFirstDueDate() == null) {
            throw new ServiceException("Poné la fecha de vencimiento de la primera cuota");
        }
        if (loan.getLoanNumber() != null && loans.existsNumber(userId, loan.getLoanNumber(), excludeId)) {
            throw new ConflictException("Ya tenés otro préstamo con ese número");
        }
    }

    /** Textos sin espacios de más; vacíos como null. */
    private static Loan clean(Loan loan) {
        return loan.toBuilder()
                .lender(blankToNull(loan.getLender()))
                .name(blankToNull(loan.getName()))
                .loanNumber(blankToNull(loan.getLoanNumber()))
                .shortReference(blankToNull(loan.getShortReference()))
                .notes(blankToNull(loan.getNotes()))
                .currency(loan.getCurrency() == null ? null : loan.getCurrency().trim().toUpperCase())
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Loan find(UUID userId, UUID loanId) {
        return loans.findByIdForUser(userId, loanId).orElseThrow(() -> new NotFoundException("préstamo"));
    }

    private LoanView view(UUID userId, Loan loan) {
        return new LoanView(loan, obligations.findByLoan(userId, loan.getId()));
    }
}
