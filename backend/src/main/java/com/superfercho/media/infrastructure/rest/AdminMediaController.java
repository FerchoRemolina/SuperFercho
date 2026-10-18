package com.superfercho.media.infrastructure.rest;

import com.superfercho.media.application.dto.StoredMedia;
import com.superfercho.media.application.usecase.UploadMediaImageUseCase;
import com.superfercho.media.infrastructure.web.ImageSniffer;
import com.superfercho.media.infrastructure.rest.dto.UploadedMediaRestResponse;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Subida de imágenes administradas (ADMIN). El campo del formulario es
 * "file"; el formato se detecta por contenido y la respuesta pública expone
 * únicamente la URL estable de la imagen.
 */
@RestController
@Profile("!test")
@RequestMapping("/api/v1/admin/media")
public class AdminMediaController {

    public static final String PUBLIC_IMAGES_BASE_URL = "/api/v1/media/images";

    private final UploadMediaImageUseCase uploadMediaImageUseCase;
    private final ImageSniffer imageSniffer;

    public AdminMediaController(
            UploadMediaImageUseCase uploadMediaImageUseCase, ImageSniffer imageSniffer) {
        this.uploadMediaImageUseCase = uploadMediaImageUseCase;
        this.imageSniffer = imageSniffer;
    }

    @PostMapping(
            value = "/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadedMediaRestResponse uploadImage(
            @RequestParam("file") MultipartFile file) throws IOException {
        byte[] content = file.getBytes();
        var format = imageSniffer.detect(content);
        StoredMedia stored = uploadMediaImageUseCase.upload(content, format);
        return UploadedMediaRestResponse.from(stored, PUBLIC_IMAGES_BASE_URL);
    }
}
