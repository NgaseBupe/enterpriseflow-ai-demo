package com.enterpriseflow.document.storage;

import com.enterpriseflow.document.DocumentProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.stereotype.Component;

/** Keeps files in a local directory, named by their storage key. */
@Component
class LocalDocumentStorage implements DocumentStorage {

    private final Path root;

    LocalDocumentStorage(DocumentProperties properties) {
        this.root = properties.storageLocation().toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create document storage directory " + root, e);
        }
    }

    @Override
    public void store(String key, byte[] content) {
        Path target = resolve(key);
        Path temp = target.resolveSibling(key + ".tmp");
        try {
            // Write to a temporary file first, then move it into place, so a crash never leaves a half-written file.
            Files.write(temp, content);
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot store document " + key, e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read document " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete document " + key, e);
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.getParent().equals(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}
