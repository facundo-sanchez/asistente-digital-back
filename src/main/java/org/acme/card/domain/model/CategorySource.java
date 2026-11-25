package org.acme.card.domain.model;

/** Quién asignó la categoría. USER prevalece sobre RULE y AI (RN-019). */
public enum CategorySource {
    AI, RULE, USER
}
