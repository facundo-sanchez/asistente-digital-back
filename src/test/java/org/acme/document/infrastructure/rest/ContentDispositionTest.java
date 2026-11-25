package org.acme.document.infrastructure.rest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentDispositionTest {

    @Test
    void nombreSimple() {
        assertEquals("attachment; filename=\"factura.pdf\"; filename*=UTF-8''factura.pdf",
                ContentDisposition.of(false, "factura.pdf"));
    }

    @Test
    void acentosComillasYEspaciosNoRompenElEncabezado() {
        assertEquals("inline; filename=\"p_liza _auto_.pdf\"; filename*=UTF-8''p%C3%B3liza%20%22auto%22.pdf",
                ContentDisposition.of(true, "póliza \"auto\".pdf"));
    }
}
