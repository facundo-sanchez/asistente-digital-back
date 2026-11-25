package org.acme.document.application.model;

import java.io.InputStream;

/** Archivo de un documento listo para enviar. Quien lo recibe cierra content. */
public record DocumentFile(String filename, String mimeType, long size, InputStream content) {}
