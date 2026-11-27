package org.acme.loan.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.document.application.usecase.ExtractionReviews;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.loan.application.model.*;
import org.acme.loan.application.port.output.LoanRepository;
import org.acme.loan.domain.model.Loan;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.payment.application.usecase.RegisterPaymentUseCase;
import org.acme.payment.domain.model.Payment;
import org.acme.payment.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

/**
 * Comprobantes de préstamo leídos con la IA (módulo 13.3). El de alta completa el alta del préstamo; el de pago,
 * revisado y confirmado, fija el importe real de su cuota, registra el pago con el comprobante y guarda el saldo del banco.
 */
@ApplicationScoped
public class LoanExtractionUseCase {

    private final ExtractionReviews reviews;
    private final LoanRepository loans;
    private final ObligationRepository obligations;
    private final RegisterPaymentUseCase payments;
    private final LoanUseCase loanUseCase;
    private final ObjectMapper json;

    @Inject
    public LoanExtractionUseCase(ExtractionReviews reviews, LoanRepository loans, ObligationRepository obligations, RegisterPaymentUseCase payments,
                                 LoanUseCase loanUseCase, ObjectMapper json) {
        this.reviews = reviews;
        this.loans = loans;
        this.obligations = obligations;
        this.payments = payments;
        this.loanUseCase = loanUseCase;
        this.json = json;
    }

    public LoanContractReview contractReview(UUID userId, UUID extractionId) {
        AiExtractionResult result = reviews.find(userId, extractionId, ExtractedLoanContract.SCHEMA_VERSION);
        ExtractedLoanContract contract = json.convertValue(result.getExtractedData(), ExtractedLoanContract.class);
        List<String> warnings = new ArrayList<>();
        warnings.add("El comprobante de alta no trae la cantidad de cuotas, su importe ni la primera fecha: completalos.");
        if (contract.loanNumber() != null && loans.findByNumber(userId, contract.loanNumber().trim()).isPresent()) {
            warnings.add("Ya cargaste un préstamo con ese número.");
        }
        return new LoanContractReview(result.getId(), result.getDocumentId(), result.getValidationStatus(), contract, warnings);
    }

    /** Alta del préstamo desde su comprobante: el comprobante queda como su contrato y la propuesta, revisada. */
    @Transactional
    public LoanView createFromContract(UUID userId, UUID extractionId, NewLoan request) {
        AiExtractionResult result = reviews.pending(userId, extractionId, ExtractedLoanContract.SCHEMA_VERSION);
        Loan loan = request.loan().toBuilder().contractDocumentId(result.getDocumentId()).build();
        LoanView view = loanUseCase.create(userId, new NewLoan(loan, request.installmentAmount(), request.paidInstallments()));
        reviews.close(userId, result, ExtractionValidationStatus.CONFIRMED, null);
        return view;
    }

    public LoanReceiptReview receiptReview(UUID userId, UUID extractionId) {
        AiExtractionResult result = reviews.find(userId, extractionId, ExtractedLoanReceipt.SCHEMA_VERSION);
        ExtractedLoanReceipt receipt = receipt(result);
        Optional<Loan> loan = Optional.ofNullable(receipt.loanNumber()).map(String::trim).flatMap(n -> loans.findByNumber(userId, n));
        List<String> warnings = new ArrayList<>();
        if (loan.isEmpty()) {
            warnings.add("No encontramos el préstamo de este comprobante: cargalo con estos datos o elegilo de la lista.");
        } else {
            if (receipt.installmentNumber() != null && receipt.pendingInstallments() != null
                    && receipt.installmentNumber() + receipt.pendingInstallments() != loan.get().getInstallmentsCount()) {
                warnings.add("El comprobante dice " + (receipt.installmentNumber() + receipt.pendingInstallments())
                        + " cuotas en total y el préstamo tiene " + loan.get().getInstallmentsCount() + ".");
            }
            if (receipt.installmentNumber() != null && result.getValidationStatus() == ExtractionValidationStatus.PENDING
                    && installment(userId, loan.get().getId(), receipt.installmentNumber())
                    .map(i -> i.effectiveStatus() == EffectiveObligationStatus.PAID).orElse(false)) {
                warnings.add("La cuota " + receipt.installmentNumber() + " ya figura pagada.");
            }
        }
        if (receipt.totalAmount() == null) {
            warnings.add("No se leyó el importe: completalo antes de confirmar.");
        }
        return new LoanReceiptReview(result.getId(), result.getDocumentId(), result.getValidationStatus(), receipt,
                loan.map(Loan::getId).orElse(null), warnings);
    }

    /** Fija el importe real de la cuota, registra el pago con el comprobante y guarda el saldo del banco. */
    @Transactional
    public LoanView confirmReceipt(UUID userId, UUID extractionId, LoanReceiptConfirmation confirmation) {
        AiExtractionResult result = reviews.pending(userId, extractionId, ExtractedLoanReceipt.SCHEMA_VERSION);
        if (confirmation.loanId() == null) {
            throw new ServiceException("Elegí el préstamo de este comprobante");
        }
        if (confirmation.amount() == null || confirmation.amount().signum() <= 0) {
            throw new ServiceException("Poné el importe pagado");
        }
        if (confirmation.paymentDate() == null) {
            throw new ServiceException("Poné la fecha de pago");
        }
        Loan loan = loans.findByIdForUser(userId, confirmation.loanId()).orElseThrow(() -> new NotFoundException("préstamo"));
        ObligationSummary installment = installment(userId, loan.getId(), confirmation.installmentNumber())
                .orElseThrow(() -> new ServiceException("El préstamo no tiene la cuota " + confirmation.installmentNumber()));
        if (installment.effectiveStatus() == EffectiveObligationStatus.PAID || installment.paidAmount().signum() > 0) {
            throw new ServiceException("La cuota " + confirmation.installmentNumber() + " ya tiene pagos registrados");
        }
        Obligation obligation = installment.obligation();
        obligation.setAmount(confirmation.amount());
        obligation.setEstimated(false);
        obligations.save(obligation);
        ExtractedLoanReceipt read = receipt(result);
        payments.register(userId, Payment.builder()
                .obligationId(obligation.getId())
                .amount(confirmation.amount())
                .currency(loan.getCurrency())
                .paymentDate(confirmation.paymentDate())
                .paymentMethod(PaymentMethod.OTHER)
                .receiptDocumentId(result.getDocumentId())
                .notes(breakdown(read))
                .build());
        if (confirmation.newBalance() != null) {
            loan.setBankBalance(confirmation.newBalance());
            loan.setBankBalanceDate(confirmation.paymentDate());
            loans.save(loan);
        }
        boolean changed = !Objects.equals(read.installmentNumber(), confirmation.installmentNumber())
                || read.totalAmount() == null || read.totalAmount().compareTo(confirmation.amount()) != 0;
        reviews.close(userId, result, changed ? ExtractionValidationStatus.CORRECTED : ExtractionValidationStatus.CONFIRMED, null);
        return loanUseCase.get(userId, loan.getId());
    }

    /** Descartar la propuesta: no se registra nada; el documento queda guardado. */
    @Transactional
    public void reject(UUID userId, UUID extractionId) {
        AiExtractionResult result = reviews.find(userId, extractionId, null);
        if (result.getValidationStatus() != ExtractionValidationStatus.PENDING) {
            throw new ServiceException("La propuesta ya fue revisada");
        }
        reviews.close(userId, result, ExtractionValidationStatus.REJECTED, null);
    }

    /** "Capital $ 150.728,02 · Intereses … · IVA … · Sellados …", lo que traiga. */
    static String breakdown(ExtractedLoanReceipt r) {
        NumberFormat money = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));
        money.setMinimumFractionDigits(2);
        money.setMaximumFractionDigits(2);
        List<String> parts = new ArrayList<>();
        Map<String, BigDecimal> items = new LinkedHashMap<>();
        items.put("Capital", r.capital());
        items.put("Intereses", r.interest());
        items.put("IVA", r.vat());
        items.put("Sellados", r.stamps());
        items.forEach((label, value) -> {
            if (value != null) parts.add(label + " " + money.format(value));
        });
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    private ExtractedLoanReceipt receipt(AiExtractionResult result) {
        return json.convertValue(result.getExtractedData(), ExtractedLoanReceipt.class);
    }

    private Optional<ObligationSummary> installment(UUID userId, UUID loanId, int number) {
        return obligations.findByLoan(userId, loanId).stream()
                .filter(i -> i.obligation().getInstallmentNumber() != null && i.obligation().getInstallmentNumber() == number).findFirst();
    }
}
