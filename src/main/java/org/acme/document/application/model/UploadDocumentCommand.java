package org.acme.document.application.model;

import org.acme.document.domain.model.DocumentSource;
import org.acme.shared.domain.model.DocumentType;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Archivo subido por el usuario, ya guardado en un temporal por el servidor HTTP.
 *
 * @param file             archivo temporal
 * @param originalFilename nombre que tenía en la máquina del usuario
 * @param title            opcional
 * @param source           de dónde vino: subido a mano o importado de Gmail
 */
public record UploadDocumentCommand(Path file, String originalFilename, DocumentType documentType, String title, UUID serviceId,
                                    DocumentSource source) {

    /** Subido a mano por el usuario. */
    public UploadDocumentCommand(Path file, String originalFilename, DocumentType documentType, String title, UUID serviceId) {
        this(file, originalFilename, documentType, title, serviceId, DocumentSource.MANUAL);
    }
}
