package org.acme.integration.application.port.output;

import org.acme.integration.application.model.EmailRule;

import java.util.List;
import java.util.UUID;

public interface EmailRuleRepository {

    /** Reglas activas de los proveedores del catálogo, por prioridad. */
    /** Las del catálogo más las de los proveedores propios del usuario. */
    List<EmailRule> findActive(UUID userId);
}
