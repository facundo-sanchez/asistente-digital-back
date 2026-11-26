package org.acme.assistant.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Un evento de la respuesta progresiva. type: "tool" (consulta una herramienta: text es su nombre),
 * "text" (un pedazo de la respuesta), "done" (message es la respuesta guardada) o "error" (text es el mensaje).
 */
@Schema(name = "AssistantStreamEvent")
public record AssistantStreamEvent(String type, String text, MessageResponse message) {

    public static AssistantStreamEvent tool(String name) {
        return new AssistantStreamEvent("tool", name, null);
    }

    public static AssistantStreamEvent text(String delta) {
        return new AssistantStreamEvent("text", delta, null);
    }

    public static AssistantStreamEvent done(MessageResponse message) {
        return new AssistantStreamEvent("done", null, message);
    }

    public static AssistantStreamEvent error(String message) {
        return new AssistantStreamEvent("error", message, null);
    }
}
