package com.superfercho.storefront.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.storefront.domain.exception.InvalidStorefrontContentException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StorefrontContentTest {

    private static final Instant NOW = Instant.parse("2026-10-08T15:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String SUPPORT_EMAIL = "SoporteSuperFercho@gmail.com";
    private static final String LINKEDIN_URL = "https://www.linkedin.com/in/luis-remolina-0hh0/";
    private static final String GITHUB_URL = "https://github.com/FerchoRemolina";

    private static StorefrontContent content(
            String title,
            String description,
            String ctaLabel,
            String imageUrl,
            HeroPresentationMode mode) {
        return StorefrontContent.reconstitute(
                title, description, ctaLabel, imageUrl, mode,
                SUPPORT_EMAIL, LINKEDIN_URL, GITHUB_URL, null,
                ADMIN_ID, NOW);
    }

    @Test
    void acceptsHeroWithImageOnly() {
        StorefrontContent content = content("", "", "", "/api/v1/media/images/a.png", HeroPresentationMode.STANDARD);

        assertEquals("", content.heroTitle());
        assertEquals("", content.heroDescription());
        assertEquals("", content.heroCtaLabel());
        assertEquals("/api/v1/media/images/a.png", content.heroImageUrl());
    }

    @Test
    void acceptsImageWithTitle() {
        StorefrontContent content =
                content("Título", "", "", "/api/v1/media/images/a.png", HeroPresentationMode.IMMERSIVE);

        assertEquals("Título", content.heroTitle());
        assertEquals("", content.heroDescription());
        assertEquals("", content.heroCtaLabel());
    }

    @Test
    void acceptsImageWithDescription() {
        StorefrontContent content =
                content("", "Descripción", "", "/api/v1/media/images/a.png", HeroPresentationMode.STANDARD);

        assertEquals("Descripción", content.heroDescription());
    }

    @Test
    void acceptsImageWithCtaLabel() {
        StorefrontContent content =
                content("", "", "Comprar", "/api/v1/media/images/a.png", HeroPresentationMode.STANDARD);

        assertEquals("Comprar", content.heroCtaLabel());
    }

    @Test
    void acceptsCompleteContent() {
        StorefrontContent content = content(
                "Título", "Descripción", "Comprar", "/api/v1/media/images/a.png", HeroPresentationMode.IMMERSIVE);

        assertEquals("Título", content.heroTitle());
        assertEquals("Descripción", content.heroDescription());
        assertEquals("Comprar", content.heroCtaLabel());
        assertEquals(HeroPresentationMode.IMMERSIVE, content.heroMode());
    }

    @Test
    void normalizesBlankTextsToEmptyString() {
        StorefrontContent content = content("   ", "\t", "  ", "/api/v1/media/images/a.png", HeroPresentationMode.STANDARD);

        assertEquals("", content.heroTitle());
        assertEquals("", content.heroDescription());
        assertEquals("", content.heroCtaLabel());
    }

    @Test
    void trimsTextContent() {
        StorefrontContent content =
                content("  Título  ", "  Descripción  ", "  Comprar  ", "/api/v1/media/images/a.png", HeroPresentationMode.STANDARD);

        assertEquals("Título", content.heroTitle());
        assertEquals("Descripción", content.heroDescription());
        assertEquals("Comprar", content.heroCtaLabel());
    }

    @Test
    void keepsMaximumLengthsEnforced() {
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> content("x".repeat(121), "", "", "/api/v1/media/images/a.png", HeroPresentationMode.STANDARD));
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> content("", "x".repeat(401), "", "/api/v1/media/images/a.png", HeroPresentationMode.STANDARD));
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> content("", "", "x".repeat(61), "/api/v1/media/images/a.png", HeroPresentationMode.STANDARD));

        assertEquals(120, content("x".repeat(120), "", "", null, HeroPresentationMode.STANDARD).heroTitle().length());
    }

    @Test
    void rejectsNullMode() {
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> content("Título", "", "", "/api/v1/media/images/a.png", null));
    }

    @Test
    void rejectsCompletelyEmptyHero() {
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> content("", "", "", null, HeroPresentationMode.STANDARD));
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> content("   ", "", "  ", null, HeroPresentationMode.IMMERSIVE));
    }

    @Test
    void acceptsTextOnlyHeroWithoutImage() {
        StorefrontContent content = content("Título", "Descripción", "Comprar", null, HeroPresentationMode.STANDARD);

        assertEquals("Título", content.heroTitle());
        assertEquals(null, content.heroImageUrl());
    }

    @Test
    void exposesFooterSettingsFromPersistence() {
        StorefrontContent content = content("Título", "", "", null, HeroPresentationMode.STANDARD);

        assertEquals(SUPPORT_EMAIL, content.supportEmail());
        assertEquals(LINKEDIN_URL, content.linkedinUrl());
        assertEquals(GITHUB_URL, content.githubUrl());
        assertEquals(null, content.qrImageUrl());
    }

    @Test
    void updateFooterSettingsChangesOnlyFooterFields() {
        StorefrontContent content = content("Título", "Descripción", "Comprar", null, HeroPresentationMode.STANDARD);
        StorefrontContent updated = content.updateFooterSettings(
                "soporte@nuevo.com", "https://www.linkedin.com/in/nuevo/", "https://github.com/nuevo/",
                ADMIN_ID, NOW.plusSeconds(60));

        assertEquals("soporte@nuevo.com", updated.supportEmail());
        assertEquals("https://www.linkedin.com/in/nuevo/", updated.linkedinUrl());
        assertEquals("https://github.com/nuevo/", updated.githubUrl());
        assertEquals("Título", updated.heroTitle());
    }

    @Test
    void rejectsFooterEmailWithInvalidFormat() {
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> content("Título", "", "", null, HeroPresentationMode.STANDARD)
                        .updateFooterSettings("no-es-correo", LINKEDIN_URL, GITHUB_URL, ADMIN_ID, NOW));
    }

    @Test
    void rejectsFooterLinksThatAreNotHttps() {
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> content("Título", "", "", null, HeroPresentationMode.STANDARD)
                        .updateFooterSettings(SUPPORT_EMAIL, "http://www.linkedin.com/in/x/", GITHUB_URL, ADMIN_ID, NOW));
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> content("Título", "", "", null, HeroPresentationMode.STANDARD)
                        .updateFooterSettings(SUPPORT_EMAIL, LINKEDIN_URL, "ftp://github.com/x", ADMIN_ID, NOW));
    }

    @Test
    void updateQrImageUrlAcceptsSiteMediaPathAndNull() {
        StorefrontContent content = content("Título", "", "", null, HeroPresentationMode.STANDARD);

        StorefrontContent withQr = content.updateQrImageUrl("/api/v1/media/images/qr.png", ADMIN_ID, NOW.plusSeconds(30));
        assertEquals("/api/v1/media/images/qr.png", withQr.qrImageUrl());

        StorefrontContent withoutQr = withQr.updateQrImageUrl(null, ADMIN_ID, NOW.plusSeconds(60));
        assertEquals(null, withoutQr.qrImageUrl());
    }

    @Test
    void rejectsQrImageUrlThatIsNotSiteMediaOrHttps() {
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> content("Título", "", "", null, HeroPresentationMode.STANDARD)
                        .updateQrImageUrl("http://cdn.test/qr.png", ADMIN_ID, NOW));
    }
}
