package org.acme.assistant.application.usecase;

import org.acme.assistant.domain.model.AssistantMessage;
import org.acme.assistant.domain.model.Conversation;
import org.acme.assistant.domain.model.MessageRole;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.user.application.usecase.InMemoryUserRepository;
import org.acme.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssistantUseCaseTest {

    // 02:00 UTC del 1/12 es todavía 30/11 en Buenos Aires
    private final Clock clock = Clock.fixed(Instant.parse("2026-12-01T02:00:00Z"), ZoneOffset.UTC);
    private final FakeAssistantModel model = new FakeAssistantModel();
    private final InMemoryUserRepository users = new InMemoryUserRepository(clock);
    private AssistantUseCase assistant;
    private UUID ana;

    @BeforeEach
    void setUp() {
        assistant = new AssistantUseCase(new InMemoryConversationRepository(), new InMemoryAssistantMessageRepository(), model, users, clock);
        ana = users.save(User.builder().email("ana@mail.com").name("Ana").build()).getId();
    }

    @Test
    void guardaPreguntaYRespuestaConSusReferencias() {
        Conversation conversation = assistant.create(ana, null);

        AssistantMessage answer = assistant.send(ana, conversation.getId(), "  ¿Qué facturas no pagué?  ");

        assertEquals(MessageRole.ASSISTANT, answer.getRole());
        assertEquals(List.of("unpaidInvoices"), answer.getMetadata().get(AssistantUseCase.METADATA_TOOLS));
        var detail = assistant.get(ana, conversation.getId());
        assertEquals(2, detail.messages().size());
        assertEquals("¿Qué facturas no pagué?", detail.conversation().getTitle());
        assertEquals(ana, model.lastUser);
        assertEquals(LocalDate.of(2026, 11, 30), model.lastToday);
    }

    @Test
    void elModeloVeLosUltimos20Mensajes() {
        UUID id = assistant.create(ana, null).getId();
        for (int i = 0; i < 12; i++) {
            assistant.send(ana, id, "pregunta " + i);
        }

        assertEquals(AssistantUseCase.HISTORY, model.lastHistory.size());
        assertEquals("pregunta 11", model.lastHistory.getLast().getContent());
    }

    @Test
    void otroUsuarioNoVeNiUsaLaConversacion() {
        UUID id = assistant.create(ana, null).getId();
        UUID beto = users.save(User.builder().email("beto@mail.com").name("Beto").build()).getId();

        assertThrows(NotFoundException.class, () -> assistant.get(beto, id));
        assertThrows(NotFoundException.class, () -> assistant.send(beto, id, "hola"));
        assertTrue(assistant.list(beto).isEmpty());
    }

    @Test
    void borrarLaSacaDeLaLista() {
        UUID id = assistant.create(ana, null).getId();

        assistant.delete(ana, id);

        assertTrue(assistant.list(ana).isEmpty());
        assertThrows(NotFoundException.class, () -> assistant.get(ana, id));
    }
}
