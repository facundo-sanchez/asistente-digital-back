package org.acme.assistant.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.acme.assistant.domain.model.ReferenceType;
import org.acme.obligation.application.usecase.InMemoryObligationRepository;
import org.acme.obligation.application.usecase.ObligationQueryUseCase;
import org.acme.obligation.domain.model.Obligation;
import org.acme.service.application.usecase.InMemoryServiceRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssistantToolsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 12, 1);

    @Test
    void soloVeLosVencimientosDelUsuarioDeLaSesion() {
        InMemoryObligationRepository obligations = new InMemoryObligationRepository();
        UUID ana = UUID.randomUUID();
        UUID beto = UUID.randomUUID();
        UUID gas = obligations.save(obligation(ana, "Gas")).obligation().getId();
        obligations.save(obligation(beto, "Luz de Beto"));
        AssistantToolbox box = new AssistantToolbox(new ObligationQueryUseCase(obligations, new InMemoryServiceRepository()),
                null, null, null, null, null, new ObjectMapper().findAndRegisterModules(), null, null);
        AssistantTools tools = box.forUser(ana, TODAY);

        String result = tools.execute(ToolExecutionRequest.builder().name("upcomingObligations").arguments("{\"days\": 7}").build());

        assertTrue(result.contains("Gas"));
        assertFalse(result.contains("Beto"));
        assertEquals(1, tools.references().size());
        assertEquals(ReferenceType.OBLIGATION, tools.references().getFirst().type());
        assertEquals(gas, tools.references().getFirst().id());
    }

    @Test
    void unaHerramientaQueNoExisteVuelveComoError() {
        AssistantTools tools = new AssistantToolbox(null, null, null, null, null, null, new ObjectMapper(), null, null).forUser(UUID.randomUUID(), TODAY);

        assertTrue(tools.execute(ToolExecutionRequest.builder().name("deleteEverything").arguments("{}").build()).contains("error"));
    }

    private static Obligation obligation(UUID userId, String title) {
        return Obligation.builder().userId(userId).title(title).amount(new BigDecimal("1000")).currency("ARS").dueDate(TODAY.plusDays(3)).build();
    }
}
