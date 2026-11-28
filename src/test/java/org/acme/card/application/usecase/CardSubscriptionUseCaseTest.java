package org.acme.card.application.usecase;

import org.acme.card.application.model.CardSubscriptions;
import org.acme.card.application.model.SubscriptionCharge;
import org.acme.card.domain.model.CardSubscription;
import org.acme.card.domain.model.SpendingCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CardSubscriptionUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 27);
    private static final List<LocalDate> CLOSINGS = List.of(TODAY.minusMonths(3), TODAY.minusMonths(2), TODAY.minusMonths(1), TODAY);

    private final InMemoryCardSubscriptionRepository repository = new InMemoryCardSubscriptionRepository();
    private final CardSubscriptionUseCase useCase = new CardSubscriptionUseCase(repository,
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    private final UUID ana = UUID.randomUUID();
    private final UUID visa = UUID.randomUUID();
    private final List<SubscriptionCharge> charges = new ArrayList<>();

    @Test
    void reconoceLoQueSeCobraEn3De4ResumenesYAgrupaLasReferencias() {
        charge(0, "NETFLIX.COM 12345", "8000");
        charge(1, "NETFLIX.COM 67890", "8000");
        charge(3, "NETFLIX.COM 11111", "10000");
        charge(0, "SPOTIFY", "3000");
        charge(1, "SPOTIFY", "3000");
        charge(2, "USD SPOTIFY", "3000");
        charge(0, "HELADERIA", "5000");
        charge(2, "HELADERIA", "5000");

        CardSubscriptions result = list();

        assertEquals(1, result.items().size(), "Spotify tiene 2 cargos con el mismo nombre, la heladería 2 de 4");
        CardSubscription netflix = result.items().getFirst();
        assertEquals("NETFLIX.COM", netflix.merchant());
        assertEquals(0, new BigDecimal("10000").compareTo(netflix.lastAmount()));
        assertEquals(25, netflix.increase(), "10000 contra 8000");
        assertEquals(3, netflix.months());
        assertEquals(0, new BigDecimal("10000").compareTo(result.totals().getFirst().total()));
    }

    @Test
    void unCargoMuyDistintoNoCuentaYCadaMonedaVaAparte() {
        charge(0, "GIMNASIO", "20000");
        charge(1, "GIMNASIO", "20000");
        charge(2, "GIMNASIO", "90000");
        charge(0, "YOUTUBE", "5", "USD");
        charge(1, "YOUTUBE", "5", "USD");
        charge(2, "YOUTUBE", "5", "USD");
        charge(3, "YOUTUBE", "5", "USD");

        CardSubscriptions result = list();

        assertEquals(List.of("YOUTUBE"), result.items().stream().map(CardSubscription::merchant).toList());
        assertNull(result.items().getFirst().increase());
        assertEquals("USD", result.totals().getFirst().currency());
    }

    @Test
    void descartarLaOcultaParaEseUsuarioYSePuedeVolverAMostrar() {
        for (int i = 0; i < 4; i++) charge(i, "SUPERMERCADO DIA", "50000");

        useCase.dismiss(ana, "supermercado dia");
        assertTrue(list().items().isEmpty());
        assertEquals(List.of("SUPERMERCADO DIA"), list().hidden());
        assertTrue(useCase.list(UUID.randomUUID(), null).items().isEmpty(), "otro usuario no ve los consumos de Ana");

        useCase.restore(ana, "SUPERMERCADO DIA");
        assertEquals(1, list().items().size());
    }

    private CardSubscriptions list() {
        repository.charges.put(ana, charges);
        return useCase.list(ana, null);
    }

    private void charge(int closing, String merchant, String amount) {
        charge(closing, merchant, amount, "ARS");
    }

    private void charge(int closing, String merchant, String amount, String currency) {
        LocalDate date = CLOSINGS.get(closing);
        charges.add(new SubscriptionCharge(visa, "Visa", date, date.minusDays(10), merchant, SpendingCategory.SUBSCRIPTIONS, currency, new BigDecimal(amount)));
    }
}
