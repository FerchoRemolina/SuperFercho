package com.superfercho.media.infrastructure.configuration;

import com.superfercho.media.application.port.MediaStorage;
import com.superfercho.media.application.usecase.UploadMediaImageUseCase;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wiring del módulo media y servicio público de imágenes: los archivos
 * almacenados se exponen en /api/v1/media/images/** con cache agresiva
 * (nombres versionados por UUID) y sin listado de directorio.
 */
@Configuration
@EnableConfigurationProperties(MediaProperties.class)
public class MediaConfiguration implements WebMvcConfigurer {

    private final MediaProperties properties;

    public MediaConfiguration(MediaProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnBean(MediaStorage.class)
    UploadMediaImageUseCase uploadMediaImageUseCase(MediaStorage mediaStorage) {
        return new UploadMediaImageUseCase(mediaStorage);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(properties.dir()).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/api/v1/media/images/**")
                .addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());
    }
}
