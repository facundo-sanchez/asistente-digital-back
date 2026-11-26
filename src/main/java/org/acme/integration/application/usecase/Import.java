package org.acme.integration.application.usecase;

import java.util.UUID;

/** Un import a Documentos que devuelve el id del documento creado. */
@FunctionalInterface
interface Import {

    UUID run();
}
