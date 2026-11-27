package org.acme.loan.infrastructure.integration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.integration.application.model.LoanNotice;
import org.acme.integration.application.port.output.LoanNotices;
import org.acme.loan.application.usecase.LoanNoticeUseCase;
import org.acme.provider.application.port.output.ProviderRepository;

import java.util.Set;
import java.util.UUID;

/** Gmail le pasa al módulo de préstamos el aviso de cuota que leyó. */
@ApplicationScoped
public class LoanNoticesAdapter implements LoanNotices {

    @Inject
    LoanNoticeUseCase notices;

    @Inject
    ProviderRepository providers;

    @Override
    public String apply(UUID userId, UUID providerId, LoanNotice notice) {
        String lender = providers.findNames(Set.of(providerId)).get(providerId);
        return notices.apply(userId, lender, notice.installmentNumber(), notice.reference(), notice.amount(), notice.dueDate());
    }
}
