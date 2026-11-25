package org.acme.service.application.model;

import org.acme.service.domain.model.Service;

/** Un servicio con el nombre de su proveedor, listo para mostrar. */
public record ServiceView(Service service, String providerName) {}
