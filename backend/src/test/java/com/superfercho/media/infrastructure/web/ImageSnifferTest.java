package com.superfercho.media.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.media.application.MediaImageFormat;
import com.superfercho.media.application.exception.UnsupportedMediaImageException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ImageSnifferTest {

    private final ImageSniffer sniffer = new ImageSniffer();

    @Test
    void detectsPngBySignature() {
        byte[] png = signature(new int[] {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        assertEquals(MediaImageFormat.PNG, sniffer.detect(png));
    }

    @Test
    void detectsJpegBySignature() {
        byte[] jpeg = signature(new int[] {0xFF, 0xD8, 0xFF, 0xE0});
        assertEquals(MediaImageFormat.JPEG, sniffer.detect(jpeg));
    }

    @Test
    void detectsWebpBySignature() {
        byte[] webp = new byte[30];
        webp[0] = 'R';
        webp[1] = 'I';
        webp[2] = 'F';
        webp[3] = 'F';
        webp[8] = 'W';
        webp[9] = 'E';
        webp[10] = 'B';
        webp[11] = 'P';
        assertEquals(MediaImageFormat.WEBP, sniffer.detect(webp));
    }

    @Test
    void rejectsZipContentRenamedAsImage() {
        byte[] zip = signature(new int[] {0x50, 0x4B, 0x03, 0x04});
        assertThrows(UnsupportedMediaImageException.class, () -> sniffer.detect(zip));
    }

    @Test
    void rejectsPlainTextContent() {
        assertThrows(
                UnsupportedMediaImageException.class,
                () -> sniffer.detect("esto no es una imagen".getBytes()));
    }

    @Test
    void rejectsEmptyContent() {
        assertThrows(UnsupportedMediaImageException.class, () -> sniffer.detect(new byte[0]));
    }

    @Test
    void rejectsContentShorterThanTwelveBytes() {
        byte[] truncated = Arrays.copyOf(signature(new int[] {0x89, 0x50, 0x4E, 0x47}), 8);
        assertThrows(UnsupportedMediaImageException.class, () -> sniffer.detect(truncated));
    }

    private static byte[] signature(int[] prefix) {
        byte[] content = new byte[32];
        for (int i = 0; i < prefix.length; i++) {
            content[i] = (byte) prefix[i];
        }
        return content;
    }
}
