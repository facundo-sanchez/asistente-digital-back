package org.acme.obligation.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.obligation.application.port.output.RecurringRuleRepository;
import org.acme.obligation.domain.model.RecurrenceOrigin;
import org.acme.obligation.domain.model.RecurringRule;
import org.acme.shared.domain.model.BillingFrequency;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Vencimientos que se repiten (CU-014), configurados por el usuario. Editar una regla afecta
 * a los próximos períodos; los vencimientos ya creados no cambian.
 */
@ApplicationScoped
public class ManageRecurringRuleUseCase {

    static final String INVALID_FREQUENCY = "Elegí cada cuánto se repite";

    private final RecurringRuleRepository rules;
    private final RecurringObligationGenerator generator;
    private final ObligationDraftValidator validator;
    private final Clock clock;

    @Inject
    public ManageRecurringRuleUseCase(RecurringRuleRepository rules, RecurringObligationGenerator generator,
                                      ObligationDraftValidator validator, Clock clock) {
        this.rules = rules;
        this.generator = generator;
        this.validator = validator;
        this.clock = clock;
    }

    /** Crea la regla y, enseguida, el vencimiento de su primera fecha (aunque falte más que LEAD_DAYS). */
    @Transactional
    public RecurringRule create(UUID userId, RecurringRule draft) {
        validate(userId, draft);
        RecurringRule rule = rules.save(clean(draft).toBuilder()
                .id(null)
                .userId(userId)
                .origin(RecurrenceOrigin.USER)
                .active(true)
                .build());
        generator.createCurrent(rule);
        generator.generate(rule, today());
        return rules.save(rule);
    }

    @Transactional
    public RecurringRule update(UUID userId, UUID ruleId, RecurringRule draft) {
        RecurringRule rule = find(userId, ruleId);
        validate(userId, draft);
        RecurringRule changes = clean(draft);
        rule.setTitle(changes.getTitle());
        rule.setObligationType(changes.getObligationType());
        rule.setServiceId(changes.getServiceId());
        rule.setFrequency(changes.getFrequency());
        rule.setNextExpectedDate(changes.getNextExpectedDate());
        rule.setDayOfMonth(changes.getDayOfMonth());
        rule.setEstimatedAmount(changes.getEstimatedAmount());
        rule.setCurrency(changes.getCurrency());
        generator.generate(rule, today());
        return rules.save(rule);
    }

    /** Desactivar deja de generar períodos; reactivar se pone al día. */
    @Transactional
    public RecurringRule changeStatus(UUID userId, UUID ruleId, boolean active) {
        RecurringRule rule = find(userId, ruleId);
        rule.setActive(active);
        generator.generate(rule, today());
        return rules.save(rule);
    }

    public RecurringRule get(UUID userId, UUID ruleId) {
        return find(userId, ruleId);
    }

    public List<RecurringRule> list(UUID userId) {
        return rules.findForUser(userId);
    }

    /** Para la tarea diaria: genera los períodos que empiezan pronto de todas las reglas activas. */
    public List<RecurringRule> dueRules() {
        return rules.findActiveDueUntil(today().plusDays(RecurringObligationGenerator.LEAD_DAYS));
    }

    private void validate(UUID userId, RecurringRule draft) {
        if (draft.getFrequency() == null || draft.getFrequency() == BillingFrequency.NONE) {
            throw new ServiceException(INVALID_FREQUENCY);
        }
        validator.validate(userId, draft.getServiceId(), draft.getEstimatedAmount(), draft.getCurrency());
    }

    /** El día de vencimiento sale de la fecha elegida: "10/12" → todos los días 10. */
    private static RecurringRule clean(RecurringRule draft) {
        return draft.toBuilder()
                .title(draft.getTitle().trim())
                .dayOfMonth(draft.getFrequency() == BillingFrequency.WEEKLY ? null : draft.getNextExpectedDate().getDayOfMonth())
                .build();
    }

    /** Una regla de otro usuario responde igual que una inexistente. */
    private RecurringRule find(UUID userId, UUID ruleId) {
        return rules.findByIdForUser(userId, ruleId).orElseThrow(() -> new NotFoundException("repetición"));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
