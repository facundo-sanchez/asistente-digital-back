package org.acme.assistant.application.usecase;

import org.acme.assistant.application.model.Answer;
import org.acme.assistant.application.model.AnswerListener;
import org.acme.assistant.application.port.output.AssistantModel;
import org.acme.assistant.domain.model.AssistantMessage;
import org.acme.assistant.domain.model.Reference;
import org.acme.assistant.domain.model.ReferenceType;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/** Responde siempre lo mismo y recuerda con qué lo llamaron. */
public class FakeAssistantModel implements AssistantModel {

    static final UUID INVOICE = UUID.randomUUID();
    UUID lastUser;
    LocalDate lastToday;
    List<AssistantMessage> lastHistory;
    /** Si no es null, la próxima respuesta falla con esta excepción. */
    RuntimeException failNext;

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public Answer answer(UUID userId, LocalDate today, ZoneId zone, List<AssistantMessage> history, AnswerListener listener) {
        lastUser = userId;
        lastToday = today;
        lastHistory = history;
        if (failNext != null) {
            RuntimeException e = failNext;
            failNext = null;
            throw e;
        }
        listener.tool("unpaidInvoices");
        listener.text("Tenés 1 ");
        listener.text("factura sin pagar.");
        return new Answer("Tenés 1 factura sin pagar.", List.of(new Reference(ReferenceType.INVOICE, INVOICE, "Camuzzi 05/26")),
                List.of("unpaidInvoices"), 120);
    }
}
