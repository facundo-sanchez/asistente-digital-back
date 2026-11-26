package org.acme.assistant.application.model;

import org.acme.assistant.domain.model.Reference;

import java.util.List;

/** Lo que devuelve el modelo: el texto, los registros que usó, las herramientas que llamó y los tokens que gastó. */
public record Answer(String text, List<Reference> references, List<String> tools, int tokens) {}
