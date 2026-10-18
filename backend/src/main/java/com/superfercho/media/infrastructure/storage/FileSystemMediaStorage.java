package com.superfercho.media.infrastructure.storage;

import com.superfercho.media.application.MediaImageFormat;
import com.superfercho.media.application.dto.StoredMedia;
import com.superfercho.media.application.port.MediaStorage;
import com.superfercho.media.infrastructure.configuration.MediaProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Almacenamiento filesystem local: escribe cada imagen como
 * {media-dir}/{uuid}{extensión canónica}. El nombre es siempre un UUID v4
 * generado server-side; el filename del cliente jamás toca el disco.
 */
@Component
public class FileSystemMediaStorage implements MediaStorage {

    private final Path baseDir;

    public FileSystemMediaStorage(MediaProperties properties) {
        this.baseDir = Path.of(properties.dir()).toAbsolutePath().normalize();
    }

    @Override
    public StoredMedia store(byte[] content, MediaImageFormat format) {
        try {
            Files.createDirectories(baseDir);
            Path target = baseDir.resolve(UUID.randomUUID() + format.extension());
            if (!target.getParent().equals(baseDir)) {
                throw new IllegalStateException("resolved media path escaped the base directory");
            }
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return StoredMedia.of(target.getFileName().toString(), format);
        } catch (IOException exception) {
            throw new UncheckedIOException("failed to store media image", exception);
        }
    }

    @Override
    public boolean delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }
        try {
            Path target = baseDir.resolve(storageKey);
            if (!target.getParent().equals(baseDir)) {
                throw new IllegalStateException("resolved media path escaped the base directory");
            }
            return Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new UncheckedIOException("failed to delete media image", exception);
        }
    }
}
