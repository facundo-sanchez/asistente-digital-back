package org.acme.shared.application.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** La página se acota aunque llegue fuera de rango. */
class PageQueryTest {

    @Test
    void acotaPaginaYTamano() {
        assertEquals(new PageQuery(0, 1), new PageQuery(-3, 0));
        assertEquals(PageQuery.MAX_SIZE, new PageQuery(0, 5000).size());
        assertEquals(new PageQuery(2, 20), new PageQuery(2, 20));
    }
}
