package org.acme.service.infrastructure.rest.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.acme.shared.domain.model.BillingFrequency;
import org.acme.shared.domain.model.ServiceCategory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceRequestTest {

    private final Validator validator = Validation.byDefaultProvider().configure()
            .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory().getValidator();

    private int errors(String paymentUrl) {
        var request = new ServiceRequest(UUID.randomUUID(), "Gas casa", null, ServiceCategory.values()[0],
                BillingFrequency.MONTHLY, null, null, null, paymentUrl, null);
        return validator.validate(request).size();
    }

    @Test
    void elLinkDePagoTieneQueSerHttps() {
        assertEquals(0, errors(null));
        assertEquals(0, errors(""));
        assertEquals(0, errors("https://www.camuzzigas.com.ar/pagar"));
        assertEquals(1, errors("http://www.camuzzigas.com.ar"));
        assertEquals(1, errors("javascript:alert(1)"));
    }
}
