package com.superfercho.assistant.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

class FerchoLimitsPropertiesBindingTest {

    @Test
    void bindsFromApplicationYmlUnderSuperferchoPrefix() throws Exception {
        FerchoLimitsProperties properties = Binder.get(environmentFromApplicationYml())
                .bind("superfercho.assistant.fercho.limits", FerchoLimitsProperties.class)
                .get();

        assertThat(properties.maxRequestsPerWindow()).isEqualTo(30);
        assertThat(properties.window()).isEqualTo(Duration.ofHours(24));
        assertThat(properties.pause()).isEqualTo(Duration.ofMinutes(30));
        assertThat(properties.recidiveWindow()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void legacyUnprefixedPathDoesNotBindFromApplicationYml() throws Exception {
        assertThat(Binder.get(environmentFromApplicationYml())
                        .bind("assistant.fercho.limits", FerchoLimitsProperties.class)
                        .isBound())
                .isFalse();
    }

    @Test
    void rejectsNonPositiveMaxRequestsWithoutDefaulting() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources()
                .addFirst(new MapPropertySource(
                        "limits",
                        Map.of(
                                "superfercho.assistant.fercho.limits.max-requests-per-window",
                                "0",
                                "superfercho.assistant.fercho.limits.window",
                                "24h",
                                "superfercho.assistant.fercho.limits.pause",
                                "30m",
                                "superfercho.assistant.fercho.limits.recidive-window",
                                "24h")));

        assertThatThrownBy(() -> Binder.get(environment)
                        .bind("superfercho.assistant.fercho.limits", FerchoLimitsProperties.class)
                        .get())
                .isInstanceOf(BindException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage(
                        "superfercho.assistant.fercho.limits.max-requests-per-window must be positive");
    }

    private static StandardEnvironment environmentFromApplicationYml() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (PropertySource<?> source : loader.load("application.yml", new ClassPathResource("application.yml"))) {
            environment.getPropertySources().addFirst(source);
        }
        return environment;
    }
}
