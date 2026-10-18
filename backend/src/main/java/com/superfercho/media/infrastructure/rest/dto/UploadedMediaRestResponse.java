package com.superfercho.media.infrastructure.rest.dto;

import com.superfercho.media.application.dto.StoredMedia;

/** Contrato público de subida: únicamente la URL pública de la imagen. */
public record UploadedMediaRestResponse(String url) {

    public static UploadedMediaRestResponse from(StoredMedia stored, String publicImagesBaseUrl) {
        return new UploadedMediaRestResponse(publicImagesBaseUrl + "/" + stored.storageKey());
    }
}
