package com.superfercho.media.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.superfercho.media.application.MediaImageFormat;
import com.superfercho.media.application.dto.StoredMedia;
import com.superfercho.media.application.exception.MediaTooLargeException;
import com.superfercho.media.application.exception.UnsupportedMediaImageException;
import com.superfercho.media.application.port.MediaStorage;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UploadMediaImageUseCaseTest {

    private MediaStorage mediaStorage;
    private UploadMediaImageUseCase useCase;

    @BeforeEach
    void setUp() {
        mediaStorage = mock(MediaStorage.class);
        useCase = new UploadMediaImageUseCase(mediaStorage);
    }

    @Test
    void storesContentAtOrBelowTwoMebibytes() {
        byte[] content = new byte[(int) UploadMediaImageUseCase.MAX_IMAGE_BYTES];
        when(mediaStorage.store(any(), any())).thenReturn(StoredMedia.of("k.webp", MediaImageFormat.WEBP));

        StoredMedia stored = useCase.upload(content, MediaImageFormat.WEBP);

        assertEquals("k.webp", stored.storageKey());
        assertEquals("image/webp", stored.contentType());
        verify(mediaStorage).store(content, MediaImageFormat.WEBP);
    }

    @Test
    void rejectsContentAboveTwoMebibytesWithoutStoring() {
        byte[] content = new byte[(int) UploadMediaImageUseCase.MAX_IMAGE_BYTES + 1];

        assertThrows(MediaTooLargeException.class, () -> useCase.upload(content, MediaImageFormat.PNG));
        verifyNoInteractions(mediaStorage);
    }

    @Test
    void rejectsEmptyContent() {
        assertThrows(
                UnsupportedMediaImageException.class, () -> useCase.upload(new byte[0], MediaImageFormat.PNG));
        verifyNoInteractions(mediaStorage);
    }
}
