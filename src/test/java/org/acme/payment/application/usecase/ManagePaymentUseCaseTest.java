package org.acme.payment.application.usecase;

import org.acme.document.application.usecase.InMemoryDocumentRepository;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Historial, anulación y corrección de pagos (RN-008). */
class ManagePaymentUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();

    private InMemoryObligationRepository obligations;
    private InMemoryPaymentRepository payments;
    private RegisterPaymentUseCase register;
    private ManagePaymentUseCase manage;
    private UUID light;

    @BeforeEach
    void setUp() {
        obligations = new InMemoryObligationRepository();
        payments = new InMemoryPaymentRepository(obligations);
        PaymentDraftValidator validator = new PaymentDraftValidator(new InMemoryDocumentRepository());
        register = new RegisterPaymentUseCase(payments, obligations, validator);
        manage = new ManagePaymentUseCase(payments, obligations, validator);
        light = obligations.save(Obligation.builder()
                .userId(ANA).title("Luz").dueDate(LocalDate.of(2026, 12, 15))
                .amount(new BigDecimal("40000")).currency("ARS")
                .build()).obligation().getId();
    }

    @Test
    void anularElUnicoPagoVuelveAPendienteYQuedaEnElHistorial() {
        Payment paid = register.register(ANA, payment("40000", 20));
        assertEquals(ObligationStatus.PAID, status());

        manage.voidPayment(ANA, paid.getId(), " Cargado por error ");

        assertEquals(ObligationStatus.PENDING, status());
        assertNull(obligations.rows.get(light).getPaidAt());
        List<Payment> history = manage.history(ANA, light);
        assertEquals(1, history.size());
        assertTrue(history.getFirst().isVoided());
        assertEquals("Cargado por error", history.getFirst().getVoidReason());
        assertThrows(ServiceException.class, () -> manage.voidPayment(ANA, paid.getId(), "otra vez"), "ya anulado");
    }

    @Test
    void anularUnoDeDosDejaPagoParcial() {
        register.register(ANA, payment("15000", 10));
        Payment second = register.register(ANA, payment("25000", 20));

        manage.voidPayment(ANA, second.getId(), "Rechazado por el banco");

        assertEquals(ObligationStatus.PARTIALLY_PAID, status());
        assertEquals(new BigDecimal("25000"), obligations.findByIdForUser(ANA, light).orElseThrow().balance());
    }

    @Test
    void corregirAnulaElPagoYCreaElReemplazo() {
        Payment wrong = register.register(ANA, payment("4000", 10));

        Payment fixed = manage.correct(ANA, wrong.getId(), payment("40000", 10));

        assertEquals(wrong.getId(), fixed.getReplacesPaymentId());
        assertEquals(ObligationStatus.PAID, status());
        List<Payment> history = manage.history(ANA, light);
        assertEquals(2, history.size());
        assertEquals("Corregido", history.stream().filter(Payment::isVoided).findFirst().orElseThrow().getVoidReason());
        assertThrows(ServiceException.class, () -> manage.correct(ANA, fixed.getId(), payment("100", 10).toBuilder().currency("USD").build()));
    }

    @Test
    void unPagoUObligacionDeOtroUsuarioRespondeComoInexistente() {
        Payment paid = register.register(ANA, payment("100", 10));

        assertThrows(NotFoundException.class, () -> manage.voidPayment(BETO, paid.getId(), "x"));
        assertThrows(NotFoundException.class, () -> manage.correct(BETO, paid.getId(), payment("1", 10)));
        assertThrows(NotFoundException.class, () -> manage.history(BETO, light));
        assertFalse(payments.rows.getFirst().isVoided());
    }

    private ObligationStatus status() {
        return obligations.rows.get(light).getStatus();
    }

    private Payment payment(String amount, int day) {
        return Payment.builder()
                .obligationId(light).amount(new BigDecimal(amount)).currency("ARS")
                .paymentDate(LocalDate.of(2026, 11, day)).paymentMethod(PaymentMethod.CASH)
                .build();
    }
}
