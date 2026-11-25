package org.acme.shared.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** El texto de búsqueda se usa literal en el LIKE. */
class LikePatternTest {

    @Test
    void escapaLosComodinesDelLike() {
        assertEquals("100\\%", LikePattern.escape("100%"));
        assertEquals("a\\_b", LikePattern.escape("a_b"));
        assertEquals("c\\\\d", LikePattern.escape("c\\d"));
    }

    @Test
    void armaUnPatronDeContieneEnMinusculas() {
        assertEquals("%coope%", LikePattern.contains("COOPE"));
    }
}
