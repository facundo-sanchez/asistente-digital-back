package org.acme.integration.application.model;

import java.util.List;

/** Ids de mensajes de una búsqueda; nextPageToken null si no hay más. */
public record GmailMessagePage(List<String> ids, String nextPageToken) {
}
