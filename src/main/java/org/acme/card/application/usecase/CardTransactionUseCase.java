package org.acme.card.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.card.application.port.output.CardTransactionRepository;
import org.acme.card.application.port.output.MerchantRuleRepository;
import org.acme.card.domain.model.CardMerchantRule;
import org.acme.card.domain.model.CardTransaction;
import org.acme.card.domain.model.CategorySource;
import org.acme.card.domain.model.SpendingCategory;
import org.acme.domain.exceptions.NotFoundException;

import java.util.List;
import java.util.UUID;

/** Rubros de los consumos: corregirlos y recordarlos por comercio (CU-016, RN-019). */
@ApplicationScoped
public class CardTransactionUseCase {

    private final CardTransactionRepository transactions;
    private final MerchantRuleRepository rules;

    @Inject
    public CardTransactionUseCase(CardTransactionRepository transactions, MerchantRuleRepository rules) {
        this.transactions = transactions;
        this.rules = rules;
    }

    /**
     * Cambia el rubro de un consumo. Con always, el rubro queda como regla del comercio y se aplica a sus otros consumos,
     * salvo a los que el usuario corrigió a mano.
     */
    @Transactional
    public CardTransaction recategorize(UUID userId, UUID transactionId, SpendingCategory category, boolean always) {
        CardTransaction transaction = transactions.findByIdForUser(userId, transactionId).orElseThrow(() -> new NotFoundException("movimiento"));
        transaction.recategorize(category, CategorySource.USER);
        CardTransaction saved = transactions.save(transaction);
        String merchant = transaction.getMerchantName();
        if (always && merchant != null) {
            CardMerchantRule rule = rules.findByMerchant(userId, merchant)
                    .orElseGet(() -> CardMerchantRule.builder().userId(userId).merchantName(merchant).build());
            rule.setCategory(category);
            rules.save(rule);
            for (CardTransaction other : transactions.findByMerchant(userId, merchant)) {
                if (!other.getId().equals(transactionId) && other.getCategorySource() != CategorySource.USER) {
                    other.recategorize(category, CategorySource.RULE);
                    transactions.save(other);
                }
            }
        }
        return saved;
    }

    public List<CardMerchantRule> rules(UUID userId) {
        return rules.findForUser(userId);
    }

    @Transactional
    public void deleteRule(UUID userId, UUID ruleId) {
        if (!rules.delete(userId, ruleId)) {
            throw new NotFoundException("regla");
        }
    }
}
