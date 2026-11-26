package org.acme.integration.infrastructure.google;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailHtmlTest {

    @Test
    void dejaElTextoConSaltosYSinEstilos() {
        String html = "<html><head><style>p{color:red}</style></head><body><p>Nro.&nbsp;Cuenta: 10-123</p>"
                + "<table><tr><td>Total:</td><td>$ 45.000,00</td></tr></table>Vencimiento:&#160;10/12/2026 &aacute;</body></html>";

        assertEquals("Nro. Cuenta: 10-123\nTotal: $ 45.000,00\nVencimiento: 10/12/2026 á", EmailHtml.toText(html));
    }

    @Test
    void elRemitenteEsSoloLaDireccion() {
        assertEquals("factura@factura.camuzzigas.com.ar", GmailApiClient.address("Camuzzi Gas <Factura@factura.camuzzigas.com.ar>"));
    }
}
