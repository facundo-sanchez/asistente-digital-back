package org.acme.obligation.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.obligation.application.model.ObligationView;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.obligation.domain.model.ObligationSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Editar y cancelar una obligación (CU-005). El estado de pago no se toca desde acá. */
@ApplicationScoped
public class ManageObligationUseCase {

    static final String NOT_EDITABLE = "Una obligación pagada o cancelada no se puede editar";
    static final String ALREADY_CANCELLED = "La obligación ya está cancelada";
    static final String HAS_PAYMENTS = "Tiene pagos registrados: anulalos antes de cancelarla";
    static final String AMOUNT_BELOW_PAID = "El importe no puede ser menor a lo ya pagado";

    private final ObligationRepository obligations;
    private final ObligationDraftValidator validator;

    @Inject
    public ManageObligationUseCase(ObligationRepository obligations, ObligationDraftValidator validator) {
        this.obligations = obligations;
        this.validator = validator;
    }

    @Transactional
    public ObligationView update(UUID userId, UUID obligationId, Obligation draft) {
        ObligationSummary current = find(userId, obligationId);
        Obligation obligation = current.obligation();
        if (obligation.getStatus() == ObligationStatus.PAID || obligation.getStatus() == ObligationStatus.CANCELLED) {
            throw new ServiceException(NOT_EDITABLE);
        }
        String serviceName = validator.validate(userId, draft);
        if (draft.getAmount() != null && draft.getAmount().compareTo(paid(current)) < 0) {
            throw new ServiceException(AMOUNT_BELOW_PAID);
        }

        obligation.setTitle(draft.getTitle().trim());
        obligation.setDescription(ObligationDraftValidator.clean(draft.getDescription()));
        obligation.setObligationType(draft.getObligationType());
        obligation.setServiceId(draft.getServiceId());
        obligation.setAmount(draft.getAmount());
        obligation.setCurrency(draft.getCurrency());
        obligation.setDueDate(draft.getDueDate());
        return new ObligationView(obligations.save(obligation), serviceName);
    }

    @Transactional
    public void cancel(UUID userId, UUID obligationId, String reason) {
        ObligationSummary current = find(userId, obligationId);
        Obligation obligation = current.obligation();
        if (obligation.getStatus() == ObligationStatus.CANCELLED) {
            throw new ServiceException(ALREADY_CANCELLED);
        }
        // Con pagos vigentes el saldo dejaría de tener sentido (RN-008)
        if (paid(current).signum() > 0) {
            throw new ServiceException(HAS_PAYMENTS);
        }
        obligation.cancel(ObligationDraftValidator.clean(reason), Instant.now());
        obligations.save(obligation);
    }

    private ObligationSummary find(UUID userId, UUID obligationId) {
        return obligations.findByIdForUser(userId, obligationId).orElseThrow(() -> new NotFoundException("obligación"));
    }

    private static BigDecimal paid(ObligationSummary summary) {
        return summary.paidAmount() == null ? BigDecimal.ZERO : summary.paidAmount();
    }
}
