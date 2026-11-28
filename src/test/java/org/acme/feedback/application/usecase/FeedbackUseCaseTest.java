package org.acme.feedback.application.usecase;

import org.acme.domain.exceptions.ServiceException;
import org.acme.feedback.domain.model.Feedback;
import org.acme.feedback.domain.model.FeedbackKind;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackUseCaseTest {

    private final InMemoryFeedbackRepository repository = new InMemoryFeedbackRepository();
    private final FeedbackUseCase useCase = new FeedbackUseCase(repository, Clock.fixed(Instant.parse("2026-11-27T12:00:00Z"), ZoneOffset.UTC));
    private final UUID ana = UUID.randomUUID();

    @Test
    void unComentarioNecesitaTextoPeroUnReporteDeErrorNo() {
        assertThrows(ServiceException.class, () -> useCase.send(ana, FeedbackKind.SUGGESTION, "  ", null, null, null));

        Feedback report = useCase.send(ana, FeedbackKind.ERROR_REPORT, null, "/loans", "{\"digest\":\"123\"}", "Firefox");

        assertEquals("Reporte de error", report.message());
        assertEquals("/loans", report.page());
        assertEquals(ana, report.userId());
    }

    @Test
    void recortaLoLargoYCadaUsuarioVeLoSuyo() {
        useCase.send(ana, FeedbackKind.ERROR_REPORT, "x", null, "d".repeat(20000), null);

        assertEquals(FeedbackUseCase.MAX_DETAILS, repository.saved.getFirst().details().length());
        assertEquals(1, useCase.recent(ana).size());
        assertTrue(useCase.recent(UUID.randomUUID()).isEmpty());
    }
}
