package org.acme.invoice.infrastructure.notification;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.invoice.application.port.output.PriceIncreaseAlerts;
import org.acme.invoice.domain.model.Invoice;
import org.acme.notification.application.usecase.GenerateNotificationsUseCase;

import java.math.BigDecimal;
import java.util.UUID;

@ApplicationScoped
public class PriceIncreaseAlertsAdapter implements PriceIncreaseAlerts {

    @Inject
    GenerateNotificationsUseCase notifications;

    @Override
    public void increased(Invoice invoice, String serviceName, UUID obligationId, BigDecimal percent, BigDecimal average) {
        notifications.priceIncrease(invoice.getUserId(), invoice.getId(), obligationId, serviceName, percent, invoice.getTotalAmount(), average,
                invoice.getCurrency());
    }
}
