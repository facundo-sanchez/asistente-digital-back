package org.acme.document.application.model;

import org.acme.shared.domain.model.DocumentType;

import java.nio.file.Path;

/**
 * Archivo subido por el usuario, ya guardado en un temporal por el servidor HTTP.
 *
 * @param file             archivo temporal
 * @param originalFilename nombre que tenía en la máquina del usuario
 * @param title            opcional
 */
public record UploadDocumentCommand(Path file, String originalFilename, DocumentType documentType, String title) {}
