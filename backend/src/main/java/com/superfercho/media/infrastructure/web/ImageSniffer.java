package com.superfercho.media.infrastructure.web;

import com.superfercho.media.application.MediaImageFormat;
import com.superfercho.media.application.exception.UnsupportedMediaImageException;
import org.springframework.stereotype.Component;

/**
 * Detecta el formato real de una imagen por su firma (magic bytes). Nunca
 * utiliza el filename, la extensión ni el Content-Type enviado por el cliente.
 */
@Component
public class ImageSniffer {

    public MediaImageFormat detect(byte[] content) {
        if (content == null || content.length < 12) {
            throw new UnsupportedMediaImageException();
        }
        if ((content[0] & 0xFF) == 0x89
                && content[1] == 'P'
                && content[2] == 'N'
                && content[3] == 'G'
                && (content[4] & 0xFF) == 0x0D
                && (content[5] & 0xFF) == 0x0A
                && (content[6] & 0xFF) == 0x1A
                && (content[7] & 0xFF) == 0x0A) {
            return MediaImageFormat.PNG;
        }
        if ((content[0] & 0xFF) == 0xFF
                && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF) {
            return MediaImageFormat.JPEG;
        }
        if (content[0] == 'R'
                && content[1] == 'I'
                && content[2] == 'F'
                && content[3] == 'F'
                && content[8] == 'W'
                && content[9] == 'E'
                && content[10] == 'B'
                && content[11] == 'P') {
            return MediaImageFormat.WEBP;
        }
        throw new UnsupportedMediaImageException();
    }
}
