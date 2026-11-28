package org.acme.feedback.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exceptions.ServiceException;
import org.acme.feedback.application.port.output.FeedbackRepository;
import org.acme.feedback.application.model.FeedbackEntry;
import org.acme.feedback.domain.model.Feedback;
import org.acme.feedback.domain.model.FeedbackKind;

import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Comentarios y sugerencias del usuario, y reportes de errores de la app. */
@ApplicationScoped
public class FeedbackUseCase {

    static final int MAX_MESSAGE = 2000;
    static final int MAX_DETAILS = 8000;
    static final int MAX_SHORT = 500;
    static final int RECENT = 20;

    private final FeedbackRepository repository;
    private final Clock clock;

    @Inject
    public FeedbackUseCase(FeedbackRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Feedback send(UUID userId, FeedbackKind kind, String message, String page, String details, String userAgent) {
        if (kind == null) {
            throw new ServiceException("Elegí qué querés mandar");
        }
        String text = message == null ? "" : message.strip();
        if (text.isEmpty() && kind != FeedbackKind.ERROR_REPORT) {
            throw new ServiceException("Escribí tu comentario");
        }
        // El reporte de error puede ir sin texto: lo importante son los datos técnicos
        return repository.save(new Feedback(null, userId, kind, cut(text.isEmpty() ? "Reporte de error" : text, MAX_MESSAGE),
                cut(page, MAX_SHORT), cut(details, MAX_DETAILS), cut(userAgent, MAX_SHORT), clock.instant()));
    }

    public List<Feedback> recent(UUID userId) {
        return repository.findForUser(userId, RECENT);
    }

    /** Administración: lo que mandaron todos los usuarios (el permiso lo controla el endpoint). */
    public PageResult<FeedbackEntry> all(FeedbackKind kind, PageQuery page) {
        return repository.findAll(kind, page);
    }

    /** Lo que llega del navegador se recorta en vez de rechazarse: un reporte largo no debe perderse. */
    static String cut(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
