package com.superfercho.storefront.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.infrastructure.security.IdentitySecurityConfiguration;
import com.superfercho.identity.infrastructure.security.JwtAccessTokenService;
import com.superfercho.media.application.MediaImageFormat;
import com.superfercho.media.infrastructure.web.ImageSniffer;
import com.superfercho.platform.time.ClockConfiguration;
import com.superfercho.storefront.application.dto.StorefrontHeroResult;
import com.superfercho.storefront.application.exception.StorefrontContentNotFoundException;
import com.superfercho.storefront.application.port.AdminPrincipalProvider;
import com.superfercho.storefront.application.usecase.CreateStorefrontFaqUseCase;
import com.superfercho.storefront.application.usecase.DeleteStorefrontFaqUseCase;
import com.superfercho.storefront.application.usecase.GetStorefrontFooterSettingsUseCase;
import com.superfercho.storefront.application.usecase.GetStorefrontHeroUseCase;
import com.superfercho.storefront.application.usecase.ListActiveStorefrontFaqsUseCase;
import com.superfercho.storefront.application.usecase.ListStorefrontFaqsUseCase;
import com.superfercho.storefront.application.usecase.ReorderStorefrontFaqsUseCase;
import com.superfercho.storefront.application.usecase.UpdateStorefrontFooterSettingsUseCase;
import com.superfercho.storefront.application.usecase.UpdateStorefrontFaqUseCase;
import com.superfercho.storefront.application.usecase.UpdateStorefrontHeroUseCase;
import com.superfercho.storefront.application.usecase.UpdateStorefrontQrImageUseCase;
import com.superfercho.storefront.domain.exception.InvalidStorefrontContentException;
import com.superfercho.storefront.domain.model.HeroPresentationMode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
        StorefrontContentController.class,
        AdminStorefrontContentController.class,
})
@Import({IdentitySecurityConfiguration.class, ClockConfiguration.class,
        StorefrontContentExceptionHandler.class})
@TestPropertySource(properties = {
        "superfercho.security.jwt.secret=test-only-superfercho-jwt-secret-key-32b",
        "superfercho.security.jwt.expiration=20m",
})
@AutoConfigureMockMvc(addFilters = false)
class StorefrontHeroRestControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T15:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @MockitoBean
    private GetStorefrontHeroUseCase getStorefrontHeroUseCase;

    @MockitoBean
    private UpdateStorefrontHeroUseCase updateStorefrontHeroUseCase;

    @MockitoBean
    private AdminPrincipalProvider adminPrincipalProvider;

    @MockitoBean
    private ListActiveStorefrontFaqsUseCase listActiveStorefrontFaqsUseCase;

    @MockitoBean
    private GetStorefrontFooterSettingsUseCase getStorefrontFooterSettingsUseCase;

    @MockitoBean
    private ListStorefrontFaqsUseCase listStorefrontFaqsUseCase;

    @MockitoBean
    private CreateStorefrontFaqUseCase createStorefrontFaqUseCase;

    @MockitoBean
    private UpdateStorefrontFaqUseCase updateStorefrontFaqUseCase;

    @MockitoBean
    private ReorderStorefrontFaqsUseCase reorderStorefrontFaqsUseCase;

    @MockitoBean
    private DeleteStorefrontFaqUseCase deleteStorefrontFaqUseCase;

    @MockitoBean
    private UpdateStorefrontFooterSettingsUseCase updateStorefrontFooterSettingsUseCase;

    @MockitoBean
    private UpdateStorefrontQrImageUseCase updateStorefrontQrImageUseCase;

    @MockitoBean
    private ImageSniffer imageSniffer;

    private String bearer(Role role) {
        return "Bearer " + jwtAccessTokenService.issue(
                UUID.fromString("11111111-1111-1111-1111-111111111111"), role).token();
    }

    @Test
    void shouldExposeHeroPubliclyWithoutAuthentication() throws Exception {
        when(getStorefrontHeroUseCase.execute()).thenReturn(new StorefrontHeroResult(
                "Tu súper, más cerca de ti",
                "Compra tus productos.",
                "Ver productos",
                null,
                HeroPresentationMode.STANDARD,
                NOW));

        mockMvc.perform(get("/api/v1/storefront-content/hero"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tu súper, más cerca de ti"))
                .andExpect(jsonPath("$.ctaLabel").value("Ver productos"))
                .andExpect(jsonPath("$.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.mode").value("STANDARD"))
                .andExpect(jsonPath("$.updatedAt").value(NOW.toString()));
    }

    @Test
    void shouldExposeImmersiveModeAndImageUrlInResponse() throws Exception {
        when(getStorefrontHeroUseCase.execute()).thenReturn(new StorefrontHeroResult(
                "Tu súper, más cerca de ti",
                "Compra tus productos.",
                "Ver productos",
                "https://cdn.test/hero.png",
                HeroPresentationMode.IMMERSIVE,
                NOW));

        mockMvc.perform(get("/api/v1/storefront-content/hero"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("IMMERSIVE"))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.test/hero.png"));
    }

    @Test
    void shouldReturn404WhenConfigurationIsMissing() throws Exception {
        when(getStorefrontHeroUseCase.execute())
                .thenThrow(new StorefrontContentNotFoundException("no inicializada"));

        mockMvc.perform(get("/api/v1/storefront-content/hero"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STOREFRONT_CONTENT_NOT_FOUND"));
    }

    @Test
    void shouldUpdateHeroAsAdminAndReturnConfiguration() throws Exception {
        when(adminPrincipalProvider.currentAdminUserId()).thenReturn(ADMIN_ID);
        when(updateStorefrontHeroUseCase.execute(
                com.superfercho.storefront.application.dto.UpdateStorefrontHeroCommand.of(
                        "Nuevo título", "Nueva descripción", "Nueva CTA",
                        "https://cdn.test/hero.png", "IMMERSIVE")))
                .thenReturn(new StorefrontHeroResult(
                        "Nuevo título", "Nueva descripción", "Nueva CTA",
                        "https://cdn.test/hero.png", HeroPresentationMode.IMMERSIVE, NOW));

        mockMvc.perform(put("/api/v1/admin/storefront-content/hero")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nuevo título","description":"Nueva descripción",
                                 "ctaLabel":"Nueva CTA","imageUrl":"https://cdn.test/hero.png","mode":"IMMERSIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nuevo título"))
                .andExpect(jsonPath("$.mode").value("IMMERSIVE"))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.test/hero.png"));
    }

    @Test
    void shouldUpdateHeroWithImageOnlyAndEmptyTexts() throws Exception {
        when(adminPrincipalProvider.currentAdminUserId()).thenReturn(ADMIN_ID);
        when(updateStorefrontHeroUseCase.execute(
                com.superfercho.storefront.application.dto.UpdateStorefrontHeroCommand.of(
                        "", "", "", "/api/v1/media/images/b5bc262d-3f73-45ca-b445-54e616162fc8.png", "IMMERSIVE")))
                .thenReturn(new StorefrontHeroResult(
                        "", "", "",
                        "/api/v1/media/images/b5bc262d-3f73-45ca-b445-54e616162fc8.png",
                        HeroPresentationMode.IMMERSIVE, NOW));

        mockMvc.perform(put("/api/v1/admin/storefront-content/hero")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","description":"",
                                 "ctaLabel":"","imageUrl":"/api/v1/media/images/b5bc262d-3f73-45ca-b445-54e616162fc8.png","mode":"IMMERSIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(""))
                .andExpect(jsonPath("$.description").value(""))
                .andExpect(jsonPath("$.ctaLabel").value(""))
                .andExpect(jsonPath("$.imageUrl")
                        .value("/api/v1/media/images/b5bc262d-3f73-45ca-b445-54e616162fc8.png"))
                .andExpect(jsonPath("$.mode").value("IMMERSIVE"));
    }

    @Test
    void shouldRejectCompletelyEmptyHeroWith400() throws Exception {
        when(adminPrincipalProvider.currentAdminUserId()).thenReturn(ADMIN_ID);
        when(updateStorefrontHeroUseCase.execute(
                com.superfercho.storefront.application.dto.UpdateStorefrontHeroCommand.of(
                        "", "", "", null, "STANDARD")))
                .thenThrow(new InvalidStorefrontContentException(
                        "El contenido del hero requiere una imagen o al menos un texto"));

        mockMvc.perform(put("/api/v1/admin/storefront-content/hero")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","description":"","ctaLabel":"","mode":"STANDARD"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STOREFRONT_CONTENT"));
    }

    @Test
    void exposesOnlyActiveFaqsOrderedPublicly() throws Exception {
        var active = com.superfercho.storefront.domain.model.StorefrontFaq.create(
                UUID.fromString("00000000-0000-0000-0000-00000000fa01"),
                "¿P?", "R.", 10, true, Instant.parse("2026-10-17T12:00:00Z"));
        when(listActiveStorefrontFaqsUseCase.execute()).thenReturn(java.util.List.of(active));

        mockMvc.perform(get("/api/v1/storefront-content/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(active.id().toString()))
                .andExpect(jsonPath("$[0].question").value("¿P?"))
                .andExpect(jsonPath("$[0].answer").value("R."))
                .andExpect(jsonPath("$[0].displayOrder").value(10))
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    @Test
    void exposesFooterSettingsPubliclyWithoutQr() throws Exception {
        when(getStorefrontFooterSettingsUseCase.execute()).thenReturn(
                new com.superfercho.storefront.application.dto.StorefrontFooterSettingsResult(
                        "SoporteSuperFercho@gmail.com",
                        "https://www.linkedin.com/in/luis-remolina-0hh0/",
                        "https://github.com/FerchoRemolina",
                        null));

        mockMvc.perform(get("/api/v1/storefront-content/footer-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supportEmail").value("SoporteSuperFercho@gmail.com"))
                .andExpect(jsonPath("$.linkedinUrl").value("https://www.linkedin.com/in/luis-remolina-0hh0/"))
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/FerchoRemolina"))
                .andExpect(jsonPath("$.qrImageUrl").doesNotExist());
    }

    @Test
    void adminCreatesFaqAndReturnsItWithActiveFlag() throws Exception {
        var created = com.superfercho.storefront.domain.model.StorefrontFaq.create(
                UUID.fromString("00000000-0000-0000-0000-00000000fa09"),
                "Nueva", "Respuesta", 30, true, Instant.parse("2026-10-17T12:00:00Z"));
        when(createStorefrontFaqUseCase.execute(any(
                        com.superfercho.storefront.application.dto.CreateStorefrontFaqCommand.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/v1/admin/storefront-content/faqs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"Nueva","answer":"Respuesta","displayOrder":30,"active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id().toString()))
                .andExpect(jsonPath("$.question").value("Nueva"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void adminDeletesFaq() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/storefront-content/faqs/{faqId}",
                        UUID.fromString("00000000-0000-0000-0000-00000000fa01"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isOk());

        verify(deleteStorefrontFaqUseCase).execute(
                UUID.fromString("00000000-0000-0000-0000-00000000fa01"));
    }

    @Test
    void adminDeleteUnknownFaqReturns404() throws Exception {
        UUID missing = UUID.fromString("00000000-0000-0000-0000-00000000fa99");
        org.mockito.Mockito.doThrow(new StorefrontContentNotFoundException(
                        "la pregunta frecuente no existe: " + missing))
                .when(deleteStorefrontFaqUseCase)
                .execute(missing);

        mockMvc.perform(delete("/api/v1/admin/storefront-content/faqs/{faqId}", missing)
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STOREFRONT_CONTENT_NOT_FOUND"));
    }

    @Test
    void adminReorderInvalidFaqsReturns400() throws Exception {
        when(reorderStorefrontFaqsUseCase.execute(any(
                        com.superfercho.storefront.application.dto.ReorderStorefrontFaqsCommand.class)))
                .thenThrow(new InvalidStorefrontContentException(
                        "orderedIds contiene duplicados"));

        mockMvc.perform(put("/api/v1/admin/storefront-content/faqs/order")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedIds":["00000000-0000-0000-0000-00000000fa01",
                                               "00000000-0000-0000-0000-00000000fa01"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STOREFRONT_CONTENT"));
    }

    @Test
    void adminUpdatesFooterSettings() throws Exception {
        when(adminPrincipalProvider.currentAdminUserId()).thenReturn(ADMIN_ID);
        when(updateStorefrontFooterSettingsUseCase.execute(any(
                        com.superfercho.storefront.application.dto.UpdateStorefrontFooterSettingsCommand.class)))
                .thenReturn(new com.superfercho.storefront.application.dto.StorefrontFooterSettingsResult(
                        "nuevo@correo.com",
                        "https://www.linkedin.com/in/nuevo/",
                        "https://github.com/nuevo/",
                        "/api/v1/media/images/qr.png"));

        mockMvc.perform(put("/api/v1/admin/storefront-content/footer-settings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supportEmail":"nuevo@correo.com",
                                 "linkedinUrl":"https://www.linkedin.com/in/nuevo/",
                                 "githubUrl":"https://github.com/nuevo/"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supportEmail").value("nuevo@correo.com"))
                .andExpect(jsonPath("$.qrImageUrl").value("/api/v1/media/images/qr.png"));
    }

    @Test
    void adminUploadsQrImageAndDeletesIt() throws Exception {
        when(adminPrincipalProvider.currentAdminUserId()).thenReturn(ADMIN_ID);
        when(imageSniffer.detect(any())).thenReturn(MediaImageFormat.PNG);
        when(updateStorefrontQrImageUseCase.execute(any(byte[].class), any(String.class), any(String.class)))
                .thenReturn(new com.superfercho.storefront.application.dto.StorefrontQrImageResult(
                        "/api/v1/media/images/qr.png"));
        when(updateStorefrontQrImageUseCase.removeImage())
                .thenReturn(new com.superfercho.storefront.application.dto.StorefrontQrImageResult(null));

        mockMvc.perform(multipart("/api/v1/admin/storefront-content/footer-settings/qr-image")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "qr.png", "image/png", new byte[] {1, 2, 3}))
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrImageUrl").value("/api/v1/media/images/qr.png"));

        mockMvc.perform(delete("/api/v1/admin/storefront-content/footer-settings/qr-image")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrImageUrl").doesNotExist());

        verify(updateStorefrontQrImageUseCase).removeImage();
    }
}
