package com.superfercho.media.infrastructure.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.infrastructure.security.IdentitySecurityConfiguration;
import com.superfercho.identity.infrastructure.security.JwtAccessTokenService;
import com.superfercho.media.application.MediaImageFormat;
import com.superfercho.media.application.usecase.UploadMediaImageUseCase;
import com.superfercho.media.infrastructure.configuration.MediaConfiguration;
import com.superfercho.media.infrastructure.storage.FileSystemMediaStorage;
import com.superfercho.media.infrastructure.web.ImageSniffer;
import com.superfercho.platform.time.ClockConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(controllers = AdminMediaController.class)
@Import({IdentitySecurityConfiguration.class, ClockConfiguration.class,
        MediaExceptionHandler.class, MediaConfiguration.class,
        FileSystemMediaStorage.class, ImageSniffer.class})
@TestPropertySource(properties = {
        "superfercho.security.jwt.secret=test-only-superfercho-jwt-secret-key-32b",
        "superfercho.security.jwt.expiration=20m",
})
class AdminMediaControllerIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @TempDir
    static Path mediaDir;

    @DynamicPropertySource
    static void mediaProperties(DynamicPropertyRegistry registry) {
        registry.add("superfercho.media.dir", () -> mediaDir.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @Test
    void adminUploadsPngWithFakeNameAndServerGeneratesUuidPath() throws Exception {
        String url = uploadAndReturnUrl(adminPngUpload());

        assertTrue(url.startsWith("/api/v1/media/images/"));
        assertFalse(url.contains(".."));
        String key = url.substring("/api/v1/media/images/".length());
        assertTrue(key.endsWith(".png"));
        assertEquals(key.substring(0, key.length() - ".png".length()),
                UUID.fromString(key.substring(0, key.length() - ".png".length())).toString(),
                "el nombre del archivo debe ser un UUID v4 server-side");
        assertTrue(Files.exists(mediaDir.resolve(key)));
        assertFalse(Files.exists(mediaDir.getParent() != null
                ? mediaDir.getParent().resolve("evil.png")
                : mediaDir.resolve("evil.png")));
    }

    @Test
    void adminUploadsJpegAndWebpWithCanonicalExtensions() throws Exception {
        String jpegUrl = uploadAndReturnUrl(adminUpload("vacation.jpg", "image/png", jpegBytes()));
        String webpUrl = uploadAndReturnUrl(adminUpload("banner.dat", "image/png", webpBytes()));

        assertTrue(jpegUrl.endsWith(".jpg"));
        assertTrue(webpUrl.endsWith(".webp"));
    }

    @Test
    void fakeContentTypeIsIgnoredInFavorOfRealContent() throws Exception {
        String url = uploadAndReturnUrl(
                new MockMultipartFile("file", "capture.png", MediaType.TEXT_PLAIN_VALUE, jpegBytes()));

        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void rejectsNonImageContentWith415() throws Exception {
        MockMultipartFile zip = new MockMultipartFile(
                "file", "document.png", MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[] {0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});

        mockMvc.perform(multipart("/api/v1/admin/media/images").file(zip)
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("MEDIA_TYPE_NOT_SUPPORTED"));
    }

    @Test
    void rejectsImagesAboveTwoMebibytesWith413() throws Exception {
        byte[] content = new byte[(int) UploadMediaImageUseCase.MAX_IMAGE_BYTES + 1];
        pngSignatureAt(content);
        MockMultipartFile oversize = new MockMultipartFile(
                "file", "big.png", MediaType.IMAGE_PNG_VALUE, content);

        mockMvc.perform(multipart("/api/v1/admin/media/images").file(oversize)
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("MEDIA_TOO_LARGE"));
    }

    @Test
    void traversalFileNameNeverEscapesTheMediaDirectory() throws Exception {
        MockMultipartFile evil = new MockMultipartFile(
                "file", "..\\..\\evil.png", MediaType.IMAGE_PNG_VALUE, pngBytes());

        String url = uploadAndReturnUrl(evil);

        assertFalse(url.contains(".."));
        String key = url.substring("/api/v1/media/images/".length());
        assertTrue(Files.exists(mediaDir.resolve(key)));
        assertFalse(key.contains("/"));
        assertFalse(key.contains("\\"));
    }

    @Test
    void uploadWithoutAuthenticationIsUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/media/images").file(adminPngUpload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadAsCustomerIsForbidden() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/media/images").file(adminPngUpload())
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicGetServesStoredImageWithDetectedContentTypeAndNosniff() throws Exception {
        String url = uploadAndReturnUrl(adminPngUpload());

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.header().string(
                        HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE))
                .andExpect(MockMvcResultMatchers.header().string(
                        "X-Content-Type-Options", "nosniff"))
                .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void publicGetOfMissingImageReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/media/images/00000000-0000-0000-0000-000000000099.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    void directoryListingIsNotAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/media/images/"))
                .andExpect(status().isNotFound());
    }

    private String uploadAndReturnUrl(MockMultipartFile file) throws Exception {
        String[] response = new String[1];
        mockMvc.perform(multipart("/api/v1/admin/media/images").file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andDo(result -> response[0] =
                        com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.url"));
        return response[0];
    }

    private static MockMultipartFile adminPngUpload() {
        return new MockMultipartFile("file", "hero.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
    }

    private static byte[] pngBytes() {
        byte[] content = new byte[64];
        pngSignatureAt(content);
        return content;
    }

    private static void pngSignatureAt(byte[] content) {
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, content, 0, signature.length);
    }

    private static byte[] jpegBytes() {
        byte[] content = new byte[64];
        content[0] = (byte) 0xFF;
        content[1] = (byte) 0xD8;
        content[2] = (byte) 0xFF;
        content[3] = (byte) 0xE0;
        return content;
    }

    private static byte[] webpBytes() {
        byte[] content = new byte[40];
        content[0] = 'R';
        content[1] = 'I';
        content[2] = 'F';
        content[3] = 'F';
        content[8] = 'W';
        content[9] = 'E';
        content[10] = 'B';
        content[11] = 'P';
        return content;
    }

    private static MockMultipartFile adminUpload(String filename, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", filename, contentType, bytes);
    }

    private String bearer(Role role) {
        return "Bearer " + jwtAccessTokenService.issue(USER_ID, role).token();
    }
}
