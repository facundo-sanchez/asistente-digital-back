package org.acme.assistant.application.port.output;

import org.acme.assistant.application.model.Answer;
import org.acme.assistant.domain.model.AssistantMessage;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/** El modelo de lenguaje que responde, con herramientas de solo lectura sobre los datos de userId. */
public interface AssistantModel {

    boolean available();

    /** history termina con la pregunta nueva. */
    Answer answer(UUID userId, LocalDate today, ZoneId zone, List<AssistantMessage> history);
}
