package org.acme.document.application.usecase;

import org.acme.document.application.port.output.FileStorage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Almacenamiento en memoria: clave → contenido. */
public class InMemoryFileStorage implements FileStorage {

    final Map<String, byte[]> files = new HashMap<>();

    @Override
    public void save(String key, Path source) throws IOException {
        files.put(key, Files.readAllBytes(source));
    }

    @Override
    public InputStream open(String key) {
        return new ByteArrayInputStream(files.get(key));
    }

    @Override
    public void delete(String key) {
        files.remove(key);
    }
}
