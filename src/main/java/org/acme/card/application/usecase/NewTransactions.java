package org.acme.card.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.card.application.port.output.CardTransactionRepository;
import org.acme.card.application.port.output.MerchantRuleRepository;
import org.acme.card.domain.model.CardMerchantRule;
import org.acme.card.domain.model.CardTransaction;
import org.acme.card.domain.model.CategorySource;
import org.acme.card.domain.model.SpendingCategory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lo que se hace con los consumos de un resumen antes de guardarlos: el rubro de la regla del comercio (RN-019) y
 * unir cada cuota con la anterior de la misma compra (RN-020).
 */
@ApplicationScoped
public class NewTransactions {

    /** Diferencia aceptada entre cuotas de la misma compra (redondeos del banco). */
    static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private final MerchantRuleRepository rules;
    private final CardTransactionRepository transactions;

    @Inject
    public NewTransactions(MerchantRuleRepository rules, CardTransactionRepository transactions) {
        this.rules = rules;
        this.transactions = transactions;
    }

    public void prepare(UUID userId, List<CardTransaction> fresh) {
        applyRules(userId, fresh);
        linkInstallments(userId, fresh);
    }

    private void applyRules(UUID userId, List<CardTransaction> fresh) {
        Map<String, SpendingCategory> byMerchant = rules.findForUser(userId).stream()
                .collect(Collectors.toMap(CardMerchantRule::getMerchantName, CardMerchantRule::getCategory, (a, b) -> a));
        for (CardTransaction t : fresh) {
            SpendingCategory ruled = t.getMerchantName() == null ? null : byMerchant.get(t.getMerchantName());
            if (ruled != null) {
                t.recategorize(ruled, CategorySource.RULE);
            }
        }
    }

    /**
     * La cuota n se une a la n-1 de la misma tarjeta, comercio, total de cuotas, moneda e importe (±1 %): toma su grupo,
     * o lo crea si la anterior no tenía. La primera cuota arranca su propio grupo.
     */
    private void linkInstallments(UUID userId, List<CardTransaction> fresh) {
        for (CardTransaction t : fresh) {
            if (t.getInstallmentTotal() == null || t.getInstallmentTotal() < 2 || t.getAmount().signum() <= 0) {
                continue;
            }
            if (t.getInstallmentNumber() > 1) {
                transactions.findInstallment(userId, t.getCardId(), t.getMerchantName(), t.getInstallmentTotal(), t.getInstallmentNumber() - 1)
                        .stream().filter(previous -> sameAmount(previous, t)).findFirst()
                        .ifPresent(previous -> {
                            if (previous.getPurchaseGroupId() == null) {
                                previous.setPurchaseGroupId(UUID.randomUUID());
                                transactions.save(previous);
                            }
                            t.setPurchaseGroupId(previous.getPurchaseGroupId());
                        });
            }
            if (t.getPurchaseGroupId() == null) {
                t.setPurchaseGroupId(UUID.randomUUID());
            }
        }
    }

    static boolean sameAmount(CardTransaction a, CardTransaction b) {
        if (!Objects.equals(a.getCurrency(), b.getCurrency())) return false;
        BigDecimal diff = a.getAmount().subtract(b.getAmount()).abs();
        return diff.compareTo(a.getAmount().abs().multiply(TOLERANCE)) <= 0;
    }
}
