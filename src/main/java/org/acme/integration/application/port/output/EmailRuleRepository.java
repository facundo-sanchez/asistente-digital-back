package org.acme.integration.application.port.output;

import org.acme.integration.application.model.EmailRule;

import java.util.List;

public interface EmailRuleRepository {

    /** Reglas activas de los proveedores del catálogo, por prioridad. */
    List<EmailRule> findActive();
}
