package org.acme.obligation.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.obligation.application.model.ObligationView;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationStatus;

import java.util.UUID;

/** Alta manual de una obligación (CU-005): VTV, impuestos, seguros… */
@ApplicationScoped
public class RegisterObligationUseCase {

    private final ObligationRepository obligations;
    private final ObligationDraftValidator validator;

    @Inject
    public RegisterObligationUseCase(ObligationRepository obligations, ObligationDraftValidator validator) {
        this.obligations = obligations;
        this.validator = validator;
    }

    @Transactional
    public ObligationView register(UUID userId, Obligation draft) {
        String serviceName = validator.validate(userId, draft);
        Obligation obligation = draft.toBuilder()
                .id(null)
                .userId(userId)
                .title(draft.getTitle().trim())
                .description(ObligationDraftValidator.clean(draft.getDescription()))
                .status(ObligationStatus.PENDING)
                .estimated(false)
                .build();
        return new ObligationView(obligations.save(obligation), serviceName);
    }
}
