package org.acme.document.application.model;

import org.acme.document.domain.model.Document;

/** Un documento con el nombre de su servicio (null si no está asociado), listo para mostrar. */
public record DocumentView(Document document, String serviceName) {}
