package com.superfercho.storefront.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.storefront.application.exception.StorefrontContentNotFoundException;
import com.superfercho.storefront.application.port.StorefrontContentRepository;
import com.superfercho.storefront.application.port.StorefrontFaqRepository;
import com.superfercho.storefront.application.usecase.CreateStorefrontFaqUseCase;
import com.superfercho.storefront.application.usecase.DeleteStorefrontFaqUseCase;
import com.superfercho.storefront.application.usecase.UpdateStorefrontFaqUseCase;
import com.superfercho.storefront.application.dto.CreateStorefrontFaqCommand;
import com.superfercho.storefront.application.dto.UpdateStorefrontFaqCommand;
import com.superfercho.storefront.domain.model.StorefrontContent;
import com.superfercho.storefront.domain.model.StorefrontFaq;
import com.superfercho.storefront.infrastructure.clock.SystemClockAdapter;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Persistencia real de FAQs y footer contra PostgreSQL + Flyway (incluye seed V23).
 */
@SpringBootTest(properties = "superfercho.scheduling.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class StorefrontPersistenceIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-17T18:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                            DockerImageName.parse("pgvector/pgvector:pg16")
                                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("superfercho")
                    .withUsername("superfercho")
                    .withPassword("superfercho");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("superfercho.security.jwt.secret", () -> "test-only-superfercho-jwt-secret-key-32b");
    }

    @Autowired
    private StorefrontFaqRepository faqRepository;

    @Autowired
    private StorefrontContentRepository contentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldLoadV23SeededFaqsAndFooterDefaults() {
        List<StorefrontFaq> faqs = faqRepository.findAllOrdered();
        assertThat(faqs).hasSize(5);
        assertThat(faqs).allMatch(StorefrontFaq::active);
        assertThat(faqs).extracting(StorefrontFaq::displayOrder)
                .containsExactly(10, 20, 30, 40, 50);

        StorefrontContent content = contentRepository.find().orElseThrow();
        assertThat(content.supportEmail()).isEqualTo("SoporteSuperFercho@gmail.com");
        assertThat(content.linkedinUrl())
                .isEqualTo("https://www.linkedin.com/in/luis-remolina-0hh0/");
        assertThat(content.githubUrl()).isEqualTo("https://github.com/FerchoRemolina");
        assertThat(content.qrImageUrl()).isNull();
    }

    @Test
    void shouldPersistUpdateAndDeleteFaqsRespectingActiveFilter() {
        var clock = new SystemClockAdapter(Clock.fixed(NOW, java.time.ZoneOffset.UTC));
        var create = new CreateStorefrontFaqUseCase(faqRepository, clock);
        var update = new UpdateStorefrontFaqUseCase(faqRepository, clock);
        var delete = new DeleteStorefrontFaqUseCase(faqRepository);

        StorefrontFaq created = create.execute(
                new CreateStorefrontFaqCommand("¿Extra?", "Respuesta extra", 60, true));
        assertThat(faqRepository.findById(created.id())).isPresent();
        assertThat(faqRepository.findActiveOrdered())
                .extracting(StorefrontFaq::id)
                .contains(created.id());

        StorefrontFaq deactivated = update.execute(
                new UpdateStorefrontFaqCommand(
                        created.id(), "¿Extra editada?", "Nueva respuesta", 60, false));
        assertThat(deactivated.active()).isFalse();
        assertThat(deactivated.question()).isEqualTo("¿Extra editada?");
        assertThat(faqRepository.findActiveOrdered())
                .extracting(StorefrontFaq::id)
                .doesNotContain(created.id());
        assertThat(faqRepository.findAllOrdered())
                .extracting(StorefrontFaq::id)
                .contains(created.id());

        delete.execute(created.id());
        assertThat(faqRepository.findById(created.id())).isEmpty();
        assertThatThrownBy(() -> delete.execute(created.id()))
                .isInstanceOf(StorefrontContentNotFoundException.class);
    }

    @Test
    void shouldPersistFooterSettingsAndQrReferenceWithoutLosingHero() {
        StorefrontContent before = contentRepository.find().orElseThrow();
        String heroTitle = before.heroTitle();
        String heroMode = before.heroMode().name();

        StorefrontContent updated = before.updateFooterSettings(
                "nuevo-soporte@example.com",
                "https://www.linkedin.com/in/example/",
                "https://github.com/example/",
                ADMIN_ID,
                NOW);
        updated = updated.updateQrImageUrl("/api/v1/media/images/qr-test.png", ADMIN_ID, NOW);
        contentRepository.save(updated, ADMIN_ID, NOW);

        StorefrontContent reloaded = contentRepository.find().orElseThrow();
        assertThat(reloaded.supportEmail()).isEqualTo("nuevo-soporte@example.com");
        assertThat(reloaded.linkedinUrl()).isEqualTo("https://www.linkedin.com/in/example/");
        assertThat(reloaded.githubUrl()).isEqualTo("https://github.com/example/");
        assertThat(reloaded.qrImageUrl()).isEqualTo("/api/v1/media/images/qr-test.png");
        assertThat(reloaded.heroTitle()).isEqualTo(heroTitle);
        assertThat(reloaded.heroMode().name()).isEqualTo(heroMode);
        assertThat(reloaded.updatedBy()).isEqualTo(ADMIN_ID);

        Integer faqCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM storefront.storefront_faqs", Integer.class);
        assertThat(faqCount).isGreaterThanOrEqualTo(5);
    }
}
