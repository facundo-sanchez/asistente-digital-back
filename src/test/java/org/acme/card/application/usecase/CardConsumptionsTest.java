package org.acme.card.application.usecase;

import org.acme.card.domain.model.CardMerchantRule;
import org.acme.card.domain.model.CardTransaction;
import org.acme.card.domain.model.CardTransactionType;
import org.acme.card.domain.model.CategorySource;
import org.acme.card.domain.model.SpendingCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Rubros por comercio (RN-019) y cuotas unidas entre resúmenes (RN-020). */
class CardConsumptionsTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID VISA = UUID.randomUUID();

    private final InMemoryCardTransactionRepository transactions = new InMemoryCardTransactionRepository();
    private final InMemoryMerchantRuleRepository rules = new InMemoryMerchantRuleRepository();
    private final NewTransactions newTransactions = new NewTransactions(rules, transactions);
    private final CardTransactionUseCase useCase = new CardTransactionUseCase(transactions, rules);

    @Test
    void laReglaDelComercioGanaAlRubroDeLaIa() {
        rules.save(CardMerchantRule.builder().userId(ANA).merchantName("YPF").category(SpendingCategory.FUEL).build());
        CardTransaction ypf = purchase("YPF", "10000", SpendingCategory.SUPERMARKET);
        CardTransaction coto = purchase("COTO", "5000", SpendingCategory.SUPERMARKET);

        newTransactions.prepare(ANA, List.of(ypf, coto));

        assertEquals(SpendingCategory.FUEL, ypf.getCategory());
        assertEquals(CategorySource.RULE, ypf.getCategorySource());
        assertEquals(SpendingCategory.SUPERMARKET, coto.getCategory());
        assertEquals(CategorySource.AI, coto.getCategorySource());
    }

    @Test
    void aplicarSiempreCreaLaReglaYNoPisaLoCorregidoAMano() {
        transactions.saveAll(List.of(purchase("YPF", "1", SpendingCategory.OTHER), purchase("YPF", "2", SpendingCategory.OTHER),
                purchase("YPF", "3", SpendingCategory.OTHER)));
        UUID first = transactions.rows.get(0).getId();
        UUID handPicked = transactions.rows.get(2).getId();
        useCase.recategorize(ANA, handPicked, SpendingCategory.TRAVEL, false);

        useCase.recategorize(ANA, first, SpendingCategory.FUEL, true);

        assertEquals(List.of(SpendingCategory.FUEL, SpendingCategory.FUEL, SpendingCategory.TRAVEL),
                transactions.rows.stream().map(CardTransaction::getCategory).toList());
        assertEquals(SpendingCategory.FUEL, rules.findByMerchant(ANA, "YPF").orElseThrow().getCategory());
        assertThrows(RuntimeException.class, () -> useCase.recategorize(UUID.randomUUID(), first, SpendingCategory.HOME, false), "de otro usuario");
    }

    @Test
    void laCuotaSeUneConLaAnteriorDeLaMismaCompra() {
        CardTransaction first = installment(VISA, "TIENDA Y", "18000", 1);
        newTransactions.prepare(ANA, List.of(first));
        transactions.saveAll(List.of(first));

        CardTransaction second = installment(VISA, "TIENDA Y", "18000.50", 2);
        CardTransaction otherAmount = installment(VISA, "TIENDA Y", "25000", 2);
        CardTransaction otherCard = installment(UUID.randomUUID(), "TIENDA Y", "18000", 2);
        newTransactions.prepare(ANA, List.of(second, otherAmount, otherCard));

        assertEquals(first.getPurchaseGroupId(), second.getPurchaseGroupId());
        assertNotEquals(first.getPurchaseGroupId(), otherAmount.getPurchaseGroupId());
        assertNotEquals(first.getPurchaseGroupId(), otherCard.getPurchaseGroupId());
    }

    private CardTransaction purchase(String merchant, String amount, SpendingCategory category) {
        return CardTransaction.builder().userId(ANA).cardId(VISA).statementId(UUID.randomUUID()).transactionDate(LocalDate.of(2026, 10, 1))
                .description(merchant).merchantName(merchant).category(category).categorySource(CategorySource.AI)
                .transactionType(CardTransactionType.PURCHASE).amount(new BigDecimal(amount)).currency("ARS").build();
    }

    private CardTransaction installment(UUID card, String merchant, String amount, int number) {
        return CardTransaction.builder().userId(ANA).cardId(card).statementId(UUID.randomUUID()).transactionDate(LocalDate.of(2026, 9, 1))
                .description(merchant).merchantName(merchant).transactionType(CardTransactionType.INSTALLMENT).amount(new BigDecimal(amount))
                .currency("ARS").installmentNumber(number).installmentTotal(6).build();
    }
}
