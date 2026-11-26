package org.acme.assistant.application.model;

/** Avisa el avance de una respuesta mientras se arma: herramientas que consulta y texto a medida que llega. */
public interface AnswerListener {

    AnswerListener NONE = new AnswerListener() {};

    /** El modelo pidió consultar esta herramienta. */
    default void tool(String name) {}

    /** Un pedazo nuevo del texto de la respuesta. */
    default void text(String delta) {}
}
