package org.acme.card.application.usecase;

import org.acme.card.application.model.CardStatementView;
import org.acme.card.domain.model.Card;
import org.acme.card.domain.model.CardBrand;
import org.acme.card.domain.model.CardStatement;
import org.acme.card.domain.model.CardStatementStatus;
import org.acme.document.application.usecase.InMemoryDocumentRepository;
import org.acme.domain.exceptions.ConflictException;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.obligation.domain.model.ObligationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Resúmenes: vencimientos por moneda, repetidos, corrección, anulación y aislamiento. */
class ManageCardStatementUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();
    private static final LocalDate DUE = LocalDate.of(2027, 1, 5);

    private InMemoryObligationRepository obligations;
    private InMemoryCardRepository cards;
    private ManageCardStatementUseCase useCase;
    private UUID visa;

    @BeforeEach
    void setUp() {
        obligations = new InMemoryObligationRepository();
        cards = new InMemoryCardRepository();
        InMemoryCardStatementRepository statements = new InMemoryCardStatementRepository();
        visa = cards.save(Card.builder().userId(ANA).serviceId(UUID.randomUUID()).name("Visa Galicia").brand(CardBrand.VISA).build()).getId();
        var validator = new CardStatementDraftValidator(cards, statements, new InMemoryDocumentRepository());
        useCase = new ManageCardStatementUseCase(statements, cards, validator, new StatementObligations(obligations),
                Clock.fixed(Instant.parse("2026-12-26T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void generaUnVencimientoPorMonedaConSaldo() {
        CardStatementView view = useCase.register(ANA, statement("250000", "40"));

        assertEquals(CardStatementStatus.CONFIRMED, view.statement().getStatus());
        Obligation pesos = obligations.rows.get(view.statement().getObligationId());
        Obligation dollars = obligations.rows.get(view.statement().getObligationUsdId());
        assertEquals("Visa Galicia · resumen 2026-12", pesos.getTitle());
        assertEquals("Visa Galicia · resumen 2026-12 · dólares", dollars.getTitle());
        assertEquals("USD", dollars.getCurrency());
        assertEquals(new BigDecimal("40"), dollars.getAmount());
        assertEquals(ObligationType.CREDIT_CARD_PAYMENT, pesos.getObligationType());
        assertEquals(DUE, pesos.getDueDate());

        assertNull(useCase.register(ANA, statement("1000", "0").toBuilder().dueDate(DUE.plusMonths(1)).build()).statement().getObligationUsdId(),
                "sin saldo en dólares no hay vencimiento en dólares");
    }

    @Test
    void unSoloResumenPorTarjetaYVencimiento() {
        useCase.register(ANA, statement("250000", "0"));

        assertThrows(ConflictException.class, () -> useCase.register(ANA, statement("1", "0")));
    }

    @Test
    void corregirAgregaElDeDolaresYAnularCancelaLosDos() {
        CardStatementView created = useCase.register(ANA, statement("250000", "0"));
        UUID id = created.statement().getId();

        CardStatementView fixed = useCase.update(ANA, id, statement("260000", "15"));
        assertEquals(new BigDecimal("260000"), obligations.rows.get(fixed.statement().getObligationId()).getAmount());
        assertNotNull(fixed.statement().getObligationUsdId());

        useCase.reject(ANA, id);
        assertEquals(ObligationStatus.CANCELLED, obligations.rows.get(fixed.statement().getObligationId()).getStatus());
        assertEquals(ObligationStatus.CANCELLED, obligations.rows.get(fixed.statement().getObligationUsdId()).getStatus());
        assertThrows(ServiceException.class, () -> useCase.update(ANA, id, statement("1", "0")), "anulado no se edita");
    }

    @Test
    void conPagosNoSeCambiaYOtroUsuarioNoLoVe() {
        CardStatementView created = useCase.register(ANA, statement("250000", "0"));
        obligations.paid.put(created.statement().getObligationId(), new BigDecimal("1000"));

        assertThrows(ServiceException.class, () -> useCase.reject(ANA, created.statement().getId()));
        assertThrows(NotFoundException.class, () -> useCase.get(BETO, created.statement().getId()));
        assertThrows(ServiceException.class, () -> useCase.register(BETO, statement("1", "0")), "tarjeta ajena");
        assertEquals(created.statement().getId(),
                useCase.findByObligation(ANA, created.statement().getObligationId()).orElseThrow().statement().getId());
    }

    @Test
    void unaTarjetaDadaDeBajaNoRecibeResumenesNuevos() {
        cards.rows.get(visa).setActive(false);

        assertThrows(ServiceException.class, () -> useCase.register(ANA, statement("1", "0")));
    }

    private CardStatement statement(String pesos, String dollars) {
        return CardStatement.builder()
                .cardId(visa).closingDate(LocalDate.of(2026, 12, 24)).dueDate(DUE)
                .totalAmount(new BigDecimal(pesos)).totalAmountUsd(new BigDecimal(dollars))
                .build();
    }
}
