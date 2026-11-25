package org.acme.shared.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SearchTextTest {

    @Test
    void quitaLosEspaciosDeMas() {
        assertEquals("agua x", SearchText.normalize("  agua   x "));
    }

    @Test
    void vacioEsComoNoFiltrar() {
        assertNull(SearchText.normalize("   "));
        assertNull(SearchText.normalize(null));
    }
}
