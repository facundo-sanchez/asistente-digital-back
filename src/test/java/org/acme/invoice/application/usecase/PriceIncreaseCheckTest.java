package org.acme.invoice.application.usecase;

import org.acme.invoice.domain.model.Invoice;
import org.acme.invoice.domain.model.InvoiceStatus;
import org.acme.user.application.usecase.InMemoryUserRepository;
import org.acme.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PriceIncreaseCheckTest {

    private final InMemoryInvoiceRepository invoices = new InMemoryInvoiceRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository(Clock.systemUTC());
    private final List<BigDecimal> alerts = new ArrayList<>();
    private final PriceIncreaseCheck check = new PriceIncreaseCheck(invoices, users, (i, n, o, percent, a) -> alerts.add(percent));
    private UUID ana;
    private final UUID gas = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ana = users.save(User.builder().email("ana@mail.com").name("Ana").build()).getId();
    }

    @Test
    void avisaSiSubeMasQueElUmbralRespectoDelPromedioDeLasUltimas3() {
        invoice("20000", 1, "ARS");
        invoice("24000", 2, "ARS");
        invoice("22000", 3, "ARS");
        invoice("10000", 4, "USD"); // otra moneda: no cuenta
        Invoice nueva = invoice("30360", 5, "ARS"); // promedio 22.000 → +38 %

        Invoice result = check.check(nueva, "Gas", UUID.randomUUID());

        assertEquals(0, new BigDecimal("38.0").compareTo(result.getPriceIncrease()));
        assertEquals(1, alerts.size());
    }

    @Test
    void sinHistorialOPorDebajoDelUmbralNoAvisa() {
        assertNull(check.check(invoice("30000", 1, "ARS"), "Gas", UUID.randomUUID()).getPriceIncrease());

        invoice("20000", 2, "ARS");
        assertNull(check.check(invoice("23000", 3, "ARS"), "Gas", UUID.randomUUID()).getPriceIncrease(), "15 % con umbral 20");
        assertTrue(alerts.isEmpty());
    }

    @Test
    void respetaElUmbralDelUsuario() {
        users.stored(ana).setPriceIncreaseThreshold(10);
        invoice("20000", 1, "ARS");

        check.check(invoice("23000", 2, "ARS"), "Gas", UUID.randomUUID());

        assertEquals(1, alerts.size(), "15 % con umbral 10");
    }

    private Invoice invoice(String amount, int month, String currency) {
        return invoices.save(Invoice.builder().userId(ana).serviceId(gas).totalAmount(new BigDecimal(amount)).currency(currency)
                .dueDate(LocalDate.of(2026, month, 10)).status(InvoiceStatus.CONFIRMED).build());
    }
}
