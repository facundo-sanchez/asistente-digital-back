package org.acme.dashboard.application.usecase;

import org.acme.dashboard.application.model.SpendingTotal;
import org.acme.dashboard.application.port.output.SpendingRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Las sumas se hacen en SQL; en los tests del caso de uso no hay pagos. */
class EmptySpendingRepository implements SpendingRepository {

    @Override
    public List<SpendingTotal> paidByMonth(UUID userId, LocalDate from, LocalDate to) {
        return List.of();
    }

    @Override
    public List<SpendingTotal> paidByCategory(UUID userId, LocalDate from, LocalDate to) {
        return List.of();
    }
}
