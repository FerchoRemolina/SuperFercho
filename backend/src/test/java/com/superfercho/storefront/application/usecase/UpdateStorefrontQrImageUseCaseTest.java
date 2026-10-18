package com.superfercho.storefront.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.storefront.application.dto.StorefrontQrImageResult;
import com.superfercho.storefront.application.dto.StoredStorefrontImage;
import com.superfercho.storefront.application.exception.StorefrontContentNotFoundException;
import com.superfercho.storefront.application.port.AdminPrincipalProvider;
import com.superfercho.storefront.application.port.ClockPort;
import com.superfercho.storefront.application.port.StorefrontContentRepository;
import com.superfercho.storefront.application.port.StorefrontMediaPort;
import com.superfercho.storefront.domain.model.StorefrontContent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateStorefrontQrImageUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private StorefrontContentRepository contentRepository;

    @Mock
    private StorefrontMediaPort storefrontMediaPort;

    @Mock
    private AdminPrincipalProvider adminPrincipalProvider;

    @Mock
    private ClockPort clockPort;

    private UpdateStorefrontQrImageUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateStorefrontQrImageUseCase(
                contentRepository, storefrontMediaPort, adminPrincipalProvider, clockPort);
    }

    private StorefrontContent contentWithQr(String qrImageUrl) {
        return StorefrontContent.reconstitute(
                "Tu súper, más cerca de ti",
                "Compra tus productos.",
                "Ver productos",
                null,
                com.superfercho.storefront.domain.model.HeroPresentationMode.STANDARD,
                "SoporteSuperFercho@gmail.com",
                "https://www.linkedin.com/in/luis-remolina-0hh0/",
                "https://github.com/FerchoRemolina",
                qrImageUrl,
                ADMIN_ID,
                NOW);
    }

    @Test
    void storesImageAndSavesPublicReference() {
        when(contentRepository.find()).thenReturn(Optional.of(contentWithQr(null)));
        when(adminPrincipalProvider.currentAdminUserId()).thenReturn(ADMIN_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(storefrontMediaPort.store(new byte[] {1, 2}, ".png", "image/png"))
                .thenReturn(new StoredStorefrontImage("qr-1.png", "image/png"));
        when(contentRepository.save(any(StorefrontContent.class), any(UUID.class), any(Instant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StorefrontQrImageResult result = useCase.execute(new byte[] {1, 2}, ".png", "image/png");

        assertEquals("/api/v1/media/images/qr-1.png", result.qrImageUrl());
        verify(contentRepository).save(any(StorefrontContent.class), any(UUID.class), any(Instant.class));
    }

    @Test
    void replacesImageDeletingThePreviousFile() {
        when(contentRepository.find())
                .thenReturn(Optional.of(contentWithQr("/api/v1/media/images/old.png")));
        when(adminPrincipalProvider.currentAdminUserId()).thenReturn(ADMIN_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(storefrontMediaPort.store(new byte[] {1}, ".png", "image/png"))
                .thenReturn(new StoredStorefrontImage("new.png", "image/png"));
        when(contentRepository.save(any(StorefrontContent.class), any(UUID.class), any(Instant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(new byte[] {1}, ".png", "image/png");

        verify(storefrontMediaPort).delete("old.png");
    }

    @Test
    void keepsReplacedFileWhenHeroReferencesIt() {
        StorefrontContent content = StorefrontContent.reconstitute(
                "Título", "Descripción", "Ver productos",
                "/api/v1/media/images/shared.png",
                com.superfercho.storefront.domain.model.HeroPresentationMode.STANDARD,
                "SoporteSuperFercho@gmail.com",
                "https://www.linkedin.com/in/luis-remolina-0hh0/",
                "https://github.com/FerchoRemolina",
                "/api/v1/media/images/shared.png",
                ADMIN_ID, NOW);
        when(contentRepository.find()).thenReturn(Optional.of(content));
        when(adminPrincipalProvider.currentAdminUserId()).thenReturn(ADMIN_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(storefrontMediaPort.store(new byte[] {1}, ".png", "image/png"))
                .thenReturn(new StoredStorefrontImage("new.png", "image/png"));
        when(contentRepository.save(any(StorefrontContent.class), any(UUID.class), any(Instant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(new byte[] {1}, ".png", "image/png");

        verify(storefrontMediaPort, never()).delete("shared.png");
    }

    @Test
    void removeImageClearsReferenceAndDeletesFile() {
        when(contentRepository.find())
                .thenReturn(Optional.of(contentWithQr("/api/v1/media/images/old.png")));
        when(adminPrincipalProvider.currentAdminUserId()).thenReturn(ADMIN_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(contentRepository.save(any(StorefrontContent.class), any(UUID.class), any(Instant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StorefrontQrImageResult result = useCase.removeImage();

        assertEquals(null, result.qrImageUrl());
        verify(storefrontMediaPort).delete("old.png");
        verify(contentRepository).save(any(StorefrontContent.class), any(UUID.class), any(Instant.class));
    }

    @Test
    void removeImageWithoutQrOnlyClearsReference() {
        when(contentRepository.find()).thenReturn(Optional.of(contentWithQr(null)));
        when(adminPrincipalProvider.currentAdminUserId()).thenReturn(ADMIN_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(contentRepository.save(any(StorefrontContent.class), any(UUID.class), any(Instant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StorefrontQrImageResult result = useCase.removeImage();

        assertEquals(null, result.qrImageUrl());
        verify(storefrontMediaPort, never()).delete(any(String.class));
    }

    @Test
    void rejectsOperationWhenConfigurationIsMissing() {
        when(contentRepository.find()).thenReturn(Optional.empty());

        assertThrows(
                StorefrontContentNotFoundException.class,
                () -> useCase.execute(new byte[] {1}, ".png", "image/png"));
        verify(storefrontMediaPort, never()).store(any(byte[].class), any(String.class), any(String.class));
    }
}
