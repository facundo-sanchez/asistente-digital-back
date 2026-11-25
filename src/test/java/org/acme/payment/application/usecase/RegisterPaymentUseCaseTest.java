package org.acme.payment.application.usecase;

import org.acme.document.application.usecase.InMemoryDocumentRepository;
import org.acme.document.domain.model.Document;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.payment.domain.model.Payment;
import org.acme.payment.domain.model.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Registro de pagos y estado resultante de la obligación (CU-006). */
class RegisterPaymentUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();

    private InMemoryObligationRepository obligations;
    private InMemoryPaymentRepository payments;
    private InMemoryDocumentRepository documents;
    private RegisterPaymentUseCase useCase;
    private UUID light;

    @BeforeEach
    void setUp() {
        obligations = new InMemoryObligationRepository();
        payments = new InMemoryPaymentRepository(obligations);
        documents = new InMemoryDocumentRepository();
        useCase = new RegisterPaymentUseCase(payments, obligations, documents);
        light = obligation(ANA, "40000", "ARS");
    }

    @Test
    void pagoParcialYLuegoElRestoLaDejaPagada() {
        useCase.register(ANA, payment(light, "15000", "ARS"));
        assertEquals(ObligationStatus.PARTIALLY_PAID, status(light));
        assertEquals(new BigDecimal("25000"), obligations.findByIdForUser(ANA, light).orElseThrow().balance());

        Payment rest = useCase.register(ANA, payment(light, "25000", "ARS"));

        assertEquals(ANA, rest.getUserId());
        assertEquals(ObligationStatus.PAID, status(light));
        assertNotNull(obligations.rows.get(light).getPaidAt());
        assertThrows(ServiceException.class, () -> useCase.register(ANA, payment(light, "1", "ARS")), "ya pagada");
    }

    @Test
    void sinImporteQuedaPagadaConElPrimerPagoEnLaMonedaElegida() {
        UUID vtv = obligation(ANA, null, null);

        useCase.register(ANA, payment(vtv, "18000", "ARS"));

        assertEquals(ObligationStatus.PAID, status(vtv));
    }

    @Test
    void rechazaOtraMonedaMonedaInvalidaYObligacionCancelada() {
        assertThrows(ServiceException.class, () -> useCase.register(ANA, payment(light, "100", "USD")));
        assertThrows(ServiceException.class, () -> useCase.register(ANA, payment(obligation(ANA, null, null), "100", "EUR")));
        obligations.rows.get(light).setStatus(ObligationStatus.CANCELLED);
        assertThrows(ServiceException.class, () -> useCase.register(ANA, payment(light, "100", "ARS")));
        assertTrue(payments.rows.isEmpty());
    }

    @Test
    void obligacionYComprobanteDeOtroUsuarioNoSeAceptan() {
        assertThrows(NotFoundException.class, () -> useCase.register(BETO, payment(light, "100", "ARS")));

        UUID betoReceipt = documents.save(Document.builder().userId(BETO).originalFilename("r.pdf").checksum("c").storageKey("k").build()).getId();
        Payment withForeignReceipt = payment(light, "100", "ARS").toBuilder().receiptDocumentId(betoReceipt).build();
        assertThrows(ServiceException.class, () -> useCase.register(ANA, withForeignReceipt));

        UUID anaReceipt = documents.save(Document.builder().userId(ANA).originalFilename("a.pdf").checksum("d").storageKey("k2").build()).getId();
        Payment ok = useCase.register(ANA, payment(light, "100", "ARS").toBuilder().receiptDocumentId(anaReceipt).notes("  ").build());
        assertEquals(anaReceipt, ok.getReceiptDocumentId());
        assertNull(ok.getNotes());
    }

    private UUID obligation(UUID userId, String amount, String currency) {
        return obligations.save(Obligation.builder()
                .userId(userId).title("Luz").dueDate(LocalDate.of(2026, 12, 15))
                .amount(amount == null ? null : new BigDecimal(amount)).currency(currency)
                .build()).obligation().getId();
    }

    private ObligationStatus status(UUID id) {
        return obligations.rows.get(id).getStatus();
    }

    private static Payment payment(UUID obligationId, String amount, String currency) {
        return Payment.builder()
                .obligationId(obligationId).amount(new BigDecimal(amount)).currency(currency)
                .paymentDate(LocalDate.of(2026, 11, 25)).paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();
    }
}
