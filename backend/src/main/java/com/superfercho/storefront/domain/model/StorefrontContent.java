package com.superfercho.storefront.domain.model;

import com.superfercho.storefront.domain.exception.InvalidStorefrontContentException;
import java.time.Instant;
import java.util.UUID;

/**
 * Configuración singleton del Hero de la tienda. El diseño (dimensiones,
 * padding, tipografías, responsive) lo define SuperFercho; aquí solo se
 * persiste el contenido editable por el Admin.
 */
public final class StorefrontContent {

    public static final int TITLE_MAX_LENGTH = 120;
    public static final int DESCRIPTION_MAX_LENGTH = 400;
    public static final int CTA_LABEL_MAX_LENGTH = 60;
    public static final int IMAGE_URL_MAX_LENGTH = 1024;
    public static final int SUPPORT_EMAIL_MAX_LENGTH = 254;

    private static final String EMAIL_PATTERN =
            "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final String heroTitle;
    private final String heroDescription;
    private final String heroCtaLabel;
    private final String heroImageUrl;
    private final HeroPresentationMode heroMode;
    private final String supportEmail;
    private final String linkedinUrl;
    private final String githubUrl;
    private final String qrImageUrl;
    /** Admin que realizó la última actualización. Null solo en la fila semilla. */
    private final UUID updatedBy;
    private final Instant updatedAt;

    private StorefrontContent(
            String heroTitle,
            String heroDescription,
            String heroCtaLabel,
            String heroImageUrl,
            HeroPresentationMode heroMode,
            String supportEmail,
            String linkedinUrl,
            String githubUrl,
            String qrImageUrl,
            UUID updatedBy,
            Instant updatedAt) {
        String title = optionalText(heroTitle, "heroTitle", TITLE_MAX_LENGTH);
        String description = optionalText(heroDescription, "heroDescription", DESCRIPTION_MAX_LENGTH);
        String ctaLabel = optionalText(heroCtaLabel, "heroCtaLabel", CTA_LABEL_MAX_LENGTH);
        String imageUrl = optionalUrl(heroImageUrl);
        if (imageUrl == null && title.isEmpty() && description.isEmpty() && ctaLabel.isEmpty()) {
            throw new InvalidStorefrontContentException(
                    "El contenido del hero requiere una imagen o al menos un texto");
        }
        this.heroTitle = title;
        this.heroDescription = description;
        this.heroCtaLabel = ctaLabel;
        this.heroImageUrl = imageUrl;
        this.heroMode = requireMode(heroMode);
        this.supportEmail = requireEmail(supportEmail);
        this.linkedinUrl = requireHttpsUrl(linkedinUrl, "linkedinUrl");
        this.githubUrl = requireHttpsUrl(githubUrl, "githubUrl");
        this.qrImageUrl = optionalQrUrl(qrImageUrl);
        this.updatedBy = updatedBy;
        this.updatedAt = requireUpdatedAt(updatedAt);
    }

    public static StorefrontContent reconstitute(
            String heroTitle,
            String heroDescription,
            String heroCtaLabel,
            String heroImageUrl,
            HeroPresentationMode heroMode,
            String supportEmail,
            String linkedinUrl,
            String githubUrl,
            String qrImageUrl,
            UUID updatedBy,
            Instant updatedAt) {
        return new StorefrontContent(
                heroTitle,
                heroDescription,
                heroCtaLabel,
                heroImageUrl,
                heroMode,
                supportEmail,
                linkedinUrl,
                githubUrl,
                qrImageUrl,
                updatedBy,
                updatedAt);
    }

    public StorefrontContent update(
            String newTitle,
            String newDescription,
            String newCtaLabel,
            String newImageUrl,
            HeroPresentationMode newMode,
            UUID updatedBy,
            Instant updatedAt) {
        return new StorefrontContent(
                newTitle,
                newDescription,
                newCtaLabel,
                newImageUrl,
                newMode,
                supportEmail,
                linkedinUrl,
                githubUrl,
                qrImageUrl,
                updatedBy,
                updatedAt);
    }

    /** Actualiza únicamente los ajustes del footer (correo y enlaces sociales). */
    public StorefrontContent updateFooterSettings(
            String newSupportEmail,
            String newLinkedinUrl,
            String newGithubUrl,
            UUID updatedBy,
            Instant updatedAt) {
        return new StorefrontContent(
                heroTitle,
                heroDescription,
                heroCtaLabel,
                heroImageUrl,
                heroMode,
                newSupportEmail,
                newLinkedinUrl,
                newGithubUrl,
                qrImageUrl,
                updatedBy,
                updatedAt);
    }

    /** Reemplaza o elimina (null) la imagen del QR conservando el resto. */
    public StorefrontContent updateQrImageUrl(
            String newQrImageUrl, UUID updatedBy, Instant updatedAt) {
        return new StorefrontContent(
                heroTitle,
                heroDescription,
                heroCtaLabel,
                heroImageUrl,
                heroMode,
                supportEmail,
                linkedinUrl,
                githubUrl,
                newQrImageUrl,
                updatedBy,
                updatedAt);
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidStorefrontContentException(field + " es obligatorio");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new InvalidStorefrontContentException(
                    field + " no puede exceder " + maxLength + " caracteres");
        }
        return trimmed;
    }

    private static String requireEmail(String value) {
        String email = requireText(value, "supportEmail", SUPPORT_EMAIL_MAX_LENGTH);
        if (!email.matches(EMAIL_PATTERN)) {
            throw new InvalidStorefrontContentException(
                    "supportEmail no tiene un formato de correo válido");
        }
        return email;
    }

    private static String requireHttpsUrl(String value, String field) {
        String url = requireText(value, field, IMAGE_URL_MAX_LENGTH);
        if (!url.startsWith("https://")) {
            throw new InvalidStorefrontContentException(
                    field + " debe ser una URL HTTPS");
        }
        return url;
    }

    private static String optionalQrUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > IMAGE_URL_MAX_LENGTH) {
            throw new InvalidStorefrontContentException(
                    "qrImageUrl no puede exceder " + IMAGE_URL_MAX_LENGTH + " caracteres");
        }
        boolean publicMediaPath = trimmed.startsWith("/api/v1/media/images/");
        boolean https = trimmed.startsWith("https://");
        if (!publicMediaPath && !https) {
            throw new InvalidStorefrontContentException(
                    "qrImageUrl debe ser una ruta de imagen del sitio o una URL HTTPS");
        }
        return trimmed;
    }

    /** Textos del hero opcionales: blank/null se normaliza a "" (la BD no
     *  admite null); los máximos de longitud se mantienen. */
    private static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new InvalidStorefrontContentException(
                    field + " no puede exceder " + maxLength + " caracteres");
        }
        return trimmed;
    }

    private static String optionalUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > IMAGE_URL_MAX_LENGTH) {
            throw new InvalidStorefrontContentException(
                    "heroImageUrl no puede exceder " + IMAGE_URL_MAX_LENGTH + " caracteres");
        }
        return trimmed;
    }

    private static HeroPresentationMode requireMode(HeroPresentationMode mode) {
        if (mode == null) {
            throw new InvalidStorefrontContentException("heroMode es obligatorio");
        }
        return mode;
    }

    private static Instant requireUpdatedAt(Instant updatedAt) {
        if (updatedAt == null) {
            throw new InvalidStorefrontContentException("updatedAt es obligatorio");
        }
        return updatedAt;
    }

    public String heroTitle() {
        return heroTitle;
    }

    public String heroDescription() {
        return heroDescription;
    }

    public String heroCtaLabel() {
        return heroCtaLabel;
    }

    public String heroImageUrl() {
        return heroImageUrl;
    }

    public HeroPresentationMode heroMode() {
        return heroMode;
    }

    public String supportEmail() {
        return supportEmail;
    }

    public String linkedinUrl() {
        return linkedinUrl;
    }

    public String githubUrl() {
        return githubUrl;
    }

    public String qrImageUrl() {
        return qrImageUrl;
    }

    public UUID updatedBy() {
        return updatedBy;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
