package org.acme.document.infrastructure.rest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Encabezado Content-Disposition con el nombre original, también si tiene acentos (RFC 6266). */
public final class ContentDisposition {

    private ContentDisposition() {
    }

    public static String of(boolean inline, String filename) {
        // Versión ASCII para clientes viejos y filename* con el nombre real en UTF-8
        String ascii = filename.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_").replace("\\", "_");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return (inline ? "inline" : "attachment") + "; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }
}
