package com.superfercho.storefront.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.superfercho.storefront.application.dto.StorefrontFooterSettingsResult;
import com.superfercho.storefront.application.dto.UpdateStorefrontFooterSettingsCommand;
import com.superfercho.storefront.domain.exception.InvalidStorefrontContentException;
import com.superfercho.storefront.application.port.AdminPrincipalProvider;
import com.superfercho.storefront.application.port.ClockPort;
import com.superfercho.storefront.application.port.StorefrontContentRepository;
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
class UpdateStorefrontFooterSettingsUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private StorefrontContentRepository contentRepository;

    @Mock
    private AdminPrincipalProvider adminPrincipalProvider;

    @Mock
    private ClockPort clockPort;

    private UpdateStorefrontFooterSettingsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateStorefrontFooterSettingsUseCase(
                contentRepository, adminPrincipalProvider, clockPort);
    }

    private StorefrontContent existing() {
        return StorefrontContent.reconstitute(
                "Tu súper, más cerca de ti",
                "Compra tus productos.",
                "Ver productos",
                null,
                com.superfercho.storefront.domain.model.HeroPresentationMode.STANDARD,
                "SoporteSuperFercho@gmail.com",
                "https://www.linkedin.com/in/luis-remolina-0hh0/",
                "https://github.com/FerchoRemolina",
                null,
                ADMIN_ID,
                NOW);
    }

    @Test
    void updatesFooterSettingsKeepingHeroIntact() {
        when(contentRepository.find()).thenReturn(Optional.of(existing()));
        when(adminPrincipalProvider.currentAdminUserId()).thenReturn(ADMIN_ID);
        when(clockPort.currentTime()).thenReturn(NOW);
        when(contentRepository.save(any(StorefrontContent.class), any(UUID.class), any(Instant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StorefrontFooterSettingsResult result = useCase.execute(
                new UpdateStorefrontFooterSettingsCommand(
                        "soporte@nuevo.com",
                        "https://www.linkedin.com/in/nuevo/",
                        "https://github.com/nuevo/"));

        assertEquals("soporte@nuevo.com", result.supportEmail());
        assertEquals("https://www.linkedin.com/in/nuevo/", result.linkedinUrl());
        assertEquals("https://github.com/nuevo/", result.githubUrl());
        assertEquals(null, result.qrImageUrl());
    }

    @Test
    void rejectsInvalidEmail() {
        when(contentRepository.find()).thenReturn(Optional.of(existing()));
        when(adminPrincipalProvider.currentAdminUserId()).thenReturn(ADMIN_ID);
        when(clockPort.currentTime()).thenReturn(NOW);

        assertThrows(
                InvalidStorefrontContentException.class,
                () -> useCase.execute(new UpdateStorefrontFooterSettingsCommand(
                        "no-es-correo",
                        "https://www.linkedin.com/in/nuevo/",
                        "https://github.com/nuevo/")));
    }

    @Test
    void rejectsNonHttpsSocialLinks() {
        when(contentRepository.find()).thenReturn(Optional.of(existing()));
        when(adminPrincipalProvider.currentAdminUserId()).thenReturn(ADMIN_ID);
        when(clockPort.currentTime()).thenReturn(NOW);

        assertThrows(
                InvalidStorefrontContentException.class,
                () -> useCase.execute(new UpdateStorefrontFooterSettingsCommand(
                        "SoporteSuperFercho@gmail.com",
                        "http://www.linkedin.com/in/nuevo/",
                        "https://github.com/nuevo/")));
    }
}
