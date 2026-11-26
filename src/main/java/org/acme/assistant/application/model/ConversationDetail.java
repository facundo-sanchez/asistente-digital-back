package org.acme.assistant.application.model;

import org.acme.assistant.domain.model.AssistantMessage;
import org.acme.assistant.domain.model.Conversation;

import java.util.List;

/** Una conversación con sus mensajes, los más viejos primero. */
public record ConversationDetail(Conversation conversation, List<AssistantMessage> messages) {}
