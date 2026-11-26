package org.acme.card.infrastructure.rest.mapper;

import org.acme.card.application.model.CardStatementView;
import org.acme.card.domain.model.*;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.card.infrastructure.rest.dto.*;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.*;

@Mapper(config = MapStructConfig.class)
public interface CardRestMapper {

    // ---------- Tarjetas ----------

    CardResponse toResponse(Card card, String serviceName);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "additional", source = "additional", defaultValue = "false")
    @Mapping(target = "closingDay", ignore = true)
    @Mapping(target = "dueDay", ignore = true)
    @Mapping(target = "currency", constant = "ARS")
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Card toModel(CardRequest request);

    @InheritConfiguration(name = "toModel")
    void update(CardRequest request, @MappingTarget Card card);

    // ---------- Resúmenes ----------

    @Mapping(target = ".", source = "statement")
    CardStatementResponse toResponse(CardStatement statement, String cardName,
                                     EffectiveObligationStatus obligationStatus, EffectiveObligationStatus obligationUsdStatus);

    default CardStatementResponse toResponse(CardStatementView view) {
        return toResponse(view.statement(), view.cardName(),
                view.local() == null ? null : view.local().effectiveStatus(),
                view.usd() == null ? null : view.usd().effectiveStatus());
    }

    /** currency la toma el caso de uso de la tarjeta; status/source/obligaciones también. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "extractionResultId", ignore = true)
    @Mapping(target = "obligationId", ignore = true)
    @Mapping(target = "obligationUsdId", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "totalAmountUsd", source = "totalAmountUsd", defaultValue = "0")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "sourceMessageId", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CardStatement toModel(CardStatementRequest request);

    @InheritConfiguration(name = "toModel")
    void update(CardStatementRequest request, @MappingTarget CardStatement statement);

    StatementReconciliationResponse toResponse(StatementReconciliation reconciliation);

    // ---------- Consumos ----------

    CardTransactionResponse toResponse(CardTransaction transaction);

    /** cardId/statementId salen de la ruta; categorySource = USER lo asigna el caso de uso. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "cardId", ignore = true)
    @Mapping(target = "statementId", ignore = true)
    @Mapping(target = "categorySource", ignore = true)
    @Mapping(target = "purchaseGroupId", ignore = true)
    @Mapping(target = "confidenceScore", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CardTransaction toModel(CardTransactionRequest request);

    @InheritConfiguration(name = "toModel")
    void update(CardTransactionRequest request, @MappingTarget CardTransaction transaction);

    CardMerchantRuleResponse toResponse(CardMerchantRule rule);

    // ---------- Informe ----------

    PendingInstallmentResponse toResponse(PendingInstallment installment);

    CardSpendingReportResponse toResponse(CardSpendingReport report);
}
