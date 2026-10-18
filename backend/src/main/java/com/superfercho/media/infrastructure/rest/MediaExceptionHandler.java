package com.superfercho.media.infrastructure.rest;

import com.superfercho.media.application.exception.MediaTooLargeException;
import com.superfercho.media.application.exception.UnsupportedMediaImageException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MediaExceptionHandler {

    @ExceptionHandler({MediaTooLargeException.class, MaxUploadSizeExceededException.class})
    ProblemDetail handleTooLarge(RuntimeException ignored) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "MEDIA_TOO_LARGE");
    }

    @ExceptionHandler(UnsupportedMediaImageException.class)
    ProblemDetail handleUnsupportedType(UnsupportedMediaImageException ignored) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "MEDIA_TYPE_NOT_SUPPORTED");
    }

    private static ProblemDetail problem(HttpStatus status, String code) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        return problem;
    }
}
