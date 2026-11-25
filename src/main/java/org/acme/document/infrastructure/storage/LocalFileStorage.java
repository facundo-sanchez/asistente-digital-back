package org.acme.document.infrastructure.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.document.application.DocumentSettings;
import org.acme.document.application.port.output.FileStorage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Guarda los archivos en una carpeta del disco (app.documents.dir). */
@ApplicationScoped
public class LocalFileStorage implements FileStorage {

    private final Path root;

    @Inject
    public LocalFileStorage(DocumentSettings settings) {
        this.root = Path.of(settings.dir()).toAbsolutePath().normalize();
    }

    @Override
    public void save(String key, Path source) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public InputStream open(String key) throws IOException {
        return Files.newInputStream(resolve(key));
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    /** La clave nunca puede salir de la carpeta raíz. */
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Clave de archivo inválida");
        }
        return path;
    }
}
