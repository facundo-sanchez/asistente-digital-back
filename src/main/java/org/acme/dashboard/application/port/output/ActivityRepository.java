package org.acme.dashboard.application.port.output;

import org.acme.dashboard.application.model.Activity;

import java.util.List;
import java.util.UUID;

/** Lo último que registró el usuario (pagos vigentes, facturas, resúmenes, documentos), lo más nuevo primero. */
public interface ActivityRepository {

    List<Activity> latest(UUID userId, int limit);
}
