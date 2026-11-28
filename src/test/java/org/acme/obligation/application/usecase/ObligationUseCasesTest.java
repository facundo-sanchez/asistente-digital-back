package org.acme.obligation.application.usecase;

import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.obligation.application.model.ObligationFilter;
import org.acme.obligation.application.model.ObligationView;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.obligation.domain.model.ObligationType;
import org.acme.service.application.usecase.InMemoryServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.shared.application.model.PageQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Alta, consulta, edición y cancelación de obligaciones. */
class ObligationUseCasesTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();
    private static final PageQuery FIRST_PAGE = new PageQuery(0, 20);
    private static final LocalDate DEC_15 = LocalDate.of(2026, 12, 15);

    private InMemoryObligationRepository obligations;
    private RegisterObligationUseCase register;
    private ObligationQueryUseCase query;
    private ManageObligationUseCase manage;
    private UUID anaLight;
    private UUID betoGas;

    @BeforeEach
    void setUp() {
        obligations = new InMemoryObligationRepository();
        InMemoryServiceRepository services = new InMemoryServiceRepository();
        anaLight = services.save(Service.builder().userId(ANA).providerId(UUID.randomUUID()).name("Luz casa").autoDebit(true).build()).getId();
        betoGas = services.save(Service.builder().userId(BETO).providerId(UUID.randomUUID()).name("Gas Beto").build()).getId();
        ObligationDraftValidator validator = new ObligationDraftValidator(services);
        register = new RegisterObligationUseCase(obligations, validator);
        query = new ObligationQueryUseCase(obligations, services);
        manage = new ManageObligationUseCase(obligations, validator);
    }

    @Test
    void registraPendienteConElNombreDelServicio() {
        ObligationView view = register.register(ANA, draft("  Luz diciembre ", anaLight, "48500", "ARS", DEC_15));

        assertEquals("Luz casa", view.serviceName());
        Obligation saved = view.summary().obligation();
        assertEquals(ANA, saved.getUserId());
        assertEquals("Luz diciembre", saved.getTitle());
        assertEquals(ObligationStatus.PENDING, saved.getStatus());
        assertEquals(new BigDecimal("48500"), view.summary().balance());
    }

    @Test
    void rechazaServicioAjenoMonedaInvalidaEImporteSinMoneda() {
        assertThrows(ServiceException.class, () -> register.register(ANA, draft("Gas", betoGas, null, null, DEC_15)));
        assertThrows(ServiceException.class, () -> register.register(ANA, draft("Seguro", null, "100", "EUR", DEC_15)));
        assertThrows(ServiceException.class, () -> register.register(ANA, draft("Seguro", null, "100", null, DEC_15)));
        assertTrue(obligations.rows.isEmpty());
    }

    @Test
    void listaSoloLasDelUsuarioPorVencimientoYConFiltros() {
        register.register(ANA, draft("VTV auto", null, null, null, DEC_15));
        register.register(ANA, draft("Luz noviembre", anaLight, "40000", "ARS", LocalDate.of(2026, 11, 20)));
        register.register(BETO, draft("Gas Beto", betoGas, null, null, DEC_15));

        assertEquals(List.of("Luz noviembre", "VTV auto"), titles(new ObligationFilter(null, null, null, null, null, null)));
        assertEquals(List.of("VTV auto"), titles(new ObligationFilter(" vtv ", null, null, null, null, null)));
        assertEquals(List.of("Luz noviembre"), titles(new ObligationFilter(null, null, anaLight, null, null, null)));
        assertEquals(List.of("VTV auto"), titles(new ObligationFilter(null, null, null, null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31))));
        assertEquals(List.of(), titles(new ObligationFilter(null, Set.of(EffectiveObligationStatus.PAID), null, null, null, null)));
        // La luz se debita sola: la lista lo marca para mostrar "Se debita sola"; la VTV (sin servicio) no
        assertEquals(List.of(true, false), query.list(ANA, new ObligationFilter(null, null, null, null, null, null), FIRST_PAGE).content().stream()
                .map(ObligationView::serviceAutoDebit).toList());
    }

    @Test
    void unaObligacionDeOtroUsuarioRespondeComoInexistente() {
        UUID id = register.register(ANA, draft("VTV auto", null, null, null, DEC_15)).summary().obligation().getId();

        assertThrows(NotFoundException.class, () -> query.get(BETO, id));
        assertThrows(NotFoundException.class, () -> manage.update(BETO, id, draft("x", null, null, null, DEC_15)));
        assertThrows(NotFoundException.class, () -> manage.cancel(BETO, id, null));
        assertEquals("VTV auto", query.get(ANA, id).summary().obligation().getTitle());
    }

    @Test
    void editaLosDatosSinTocarElEstado() {
        UUID id = register.register(ANA, draft("Luz", anaLight, "40000", "ARS", DEC_15)).summary().obligation().getId();

        ObligationView view = manage.update(ANA, id, draft("Luz dic", null, "45000", "ARS", DEC_15.plusDays(2)));

        assertNull(view.serviceName());
        assertEquals("Luz dic", view.summary().obligation().getTitle());
        assertEquals(new BigDecimal("45000"), view.summary().obligation().getAmount());
        assertEquals(ObligationStatus.PENDING, view.summary().obligation().getStatus());
    }

    @Test
    void noSeEditaUnaPagadaNiUnImporteMenorALoPagado() {
        UUID id = register.register(ANA, draft("Luz", null, "40000", "ARS", DEC_15)).summary().obligation().getId();
        obligations.paid.put(id, new BigDecimal("30000"));

        assertThrows(ServiceException.class, () -> manage.update(ANA, id, draft("Luz", null, "20000", "ARS", DEC_15)));

        obligations.rows.get(id).setStatus(ObligationStatus.PAID);
        assertThrows(ServiceException.class, () -> manage.update(ANA, id, draft("Luz", null, "40000", "ARS", DEC_15)));
    }

    @Test
    void cancelaConMotivoPeroNoSiTienePagosOYaEstaCancelada() {
        UUID free = register.register(ANA, draft("Seguro", null, null, null, DEC_15)).summary().obligation().getId();
        UUID withPayments = register.register(ANA, draft("Luz", null, "40000", "ARS", DEC_15)).summary().obligation().getId();
        obligations.paid.put(withPayments, new BigDecimal("1000"));

        manage.cancel(ANA, free, "  Di de baja el seguro ");

        Obligation cancelled = obligations.rows.get(free);
        assertEquals(ObligationStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Di de baja el seguro", cancelled.getCancelReason());
        assertNotNull(cancelled.getCancelledAt());
        assertThrows(ServiceException.class, () -> manage.cancel(ANA, free, null));
        assertThrows(ServiceException.class, () -> manage.cancel(ANA, withPayments, null));
    }

    private List<String> titles(ObligationFilter filter) {
        return query.list(ANA, filter, FIRST_PAGE).content().stream().map(v -> v.summary().obligation().getTitle()).toList();
    }

    private static Obligation draft(String title, UUID serviceId, String amount, String currency, LocalDate dueDate) {
        return Obligation.builder()
                .title(title)
                .obligationType(ObligationType.OTHER)
                .serviceId(serviceId)
                .amount(amount == null ? null : new BigDecimal(amount))
                .currency(currency)
                .dueDate(dueDate)
                .build();
    }
}
