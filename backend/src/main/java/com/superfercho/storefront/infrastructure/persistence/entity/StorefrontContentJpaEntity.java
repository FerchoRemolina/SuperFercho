package com.superfercho.storefront.infrastructure.persistence.entity;

import com.superfercho.storefront.domain.model.HeroPresentationMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "storefront_content", schema = "storefront")
public class StorefrontContentJpaEntity {

    public static final UUID SINGLETON_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "hero_title", nullable = false, length = 120)
    private String heroTitle;

    @Column(name = "hero_description", nullable = false, length = 400)
    private String heroDescription;

    @Column(name = "hero_cta_label", nullable = false, length = 60)
    private String heroCtaLabel;

    @Column(name = "hero_image_url", length = 1024)
    private String heroImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "hero_mode", nullable = false, length = 20)
    private HeroPresentationMode heroMode;

    @Column(name = "support_email", nullable = false, length = 254)
    private String supportEmail;

    @Column(name = "linkedin_url", nullable = false, length = 1024)
    private String linkedinUrl;

    @Column(name = "github_url", nullable = false, length = 1024)
    private String githubUrl;

    @Column(name = "qr_image_url", length = 1024)
    private String qrImageUrl;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StorefrontContentJpaEntity() {}

    public StorefrontContentJpaEntity(
            UUID id,
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
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.heroTitle = heroTitle;
        this.heroDescription = heroDescription;
        this.heroCtaLabel = heroCtaLabel;
        this.heroImageUrl = heroImageUrl;
        this.heroMode = heroMode;
        this.supportEmail = supportEmail;
        this.linkedinUrl = linkedinUrl;
        this.githubUrl = githubUrl;
        this.qrImageUrl = qrImageUrl;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getHeroTitle() {
        return heroTitle;
    }

    public String getHeroDescription() {
        return heroDescription;
    }

    public String getHeroCtaLabel() {
        return heroCtaLabel;
    }

    public String getHeroImageUrl() {
        return heroImageUrl;
    }

    public HeroPresentationMode getHeroMode() {
        return heroMode;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public String getQrImageUrl() {
        return qrImageUrl;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
