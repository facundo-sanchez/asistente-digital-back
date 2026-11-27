package org.acme.payment.application.usecase;

import org.acme.document.application.usecase.InMemoryDocumentRepository;
import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.payment.domain.model.PaymentMethod;
import org.acme.service.application.usecase.InMemoryServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.shared.domain.model.ServiceCategory;
import org.acme.user.application.usecase.InMemoryUserRepository;
import org.acme.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AutoDebitUseCaseTest {

    // 12:00 UTC del 4/12 = 09:00 del 4/12 en Buenos Aires
    private final Clock clock = Clock.fixed(Instant.parse("2026-12-04T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 12, 4);
    private final InMemoryObligationRepository obligations = new InMemoryObligationRepository();
    private final InMemoryPaymentRepository payments = new InMemoryPaymentRepository(obligations);
    private final InMemoryServiceRepository services = new InMemoryServiceRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository(clock);
    private AutoDebitUseCase autoDebit;
    private UUID ana;
    private UUID luz;

    @BeforeEach
    void setUp() {
        RegisterPaymentUseCase register = new RegisterPaymentUseCase(payments, obligations, new PaymentDraftValidator(new InMemoryDocumentRepository()));
        autoDebit = new AutoDebitUseCase(users, services, obligations, register, clock);
        ana = users.save(User.builder().email("ana@mail.com").name("Ana").build()).getId();
        luz = services.save(Service.builder().userId(ana).providerId(UUID.randomUUID()).name("Luz").category(ServiceCategory.values()[0])
                .autoDebit(true).build()).getId();
    }

    @Test
    void elDiaQueVenceLoDaPorPagadoConDebito() {
        UUID due = obligation(luz, TODAY, "15000");

        assertEquals(1, autoDebit.payDue());

        var payment = payments.findForObligation(ana, due).getFirst();
        assertEquals(due, payment.getObligationId());
        assertEquals(PaymentMethod.DIRECT_DEBIT, payment.getPaymentMethod());
        assertEquals(0, new BigDecimal("15000").compareTo(payment.getAmount()));
        assertEquals(0, autoDebit.payDue(), "No paga dos veces");
    }

    @Test
    void noPagaAntesDeTiempoNiSinImporteNiServiciosSinDebito() {
        obligation(luz, TODAY.plusDays(1), "15000");
        obligation(luz, TODAY, null);
        UUID gas = services.save(Service.builder().userId(ana).providerId(UUID.randomUUID()).name("Gas").category(ServiceCategory.values()[0]).build()).getId();
        obligation(gas, TODAY, "9000");

        assertEquals(0, autoDebit.payDue());
    }

    @Test
    void recuperaDiasSinCorrerPeroNoVencidosViejos() {
        obligation(luz, TODAY.minusDays(2), "15000");
        obligation(luz, TODAY.minusDays(20), "15000");

        assertEquals(1, autoDebit.payDue());
    }

    private UUID obligation(UUID serviceId, LocalDate due, String amount) {
        return obligations.save(Obligation.builder().userId(ana).serviceId(serviceId).title("Vencimiento").dueDate(due)
                .amount(amount == null ? null : new BigDecimal(amount)).currency(amount == null ? null : "ARS").build()).obligation().getId();
    }
}
