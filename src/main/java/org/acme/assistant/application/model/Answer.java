package org.acme.assistant.application.model;

import org.acme.assistant.domain.model.Reference;

import java.util.List;

/** Lo que devuelve el modelo: el texto, los registros que usó y las herramientas que llamó. */
public record Answer(String text, List<Reference> references, List<String> tools) {}
