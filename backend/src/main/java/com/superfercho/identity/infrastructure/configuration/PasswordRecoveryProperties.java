package com.superfercho.identity.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import java.time.Duration;

@ConfigurationProperties(prefix = "superfercho.password-recovery")
public record PasswordRecoveryProperties(
        @DefaultValue("15m") Duration tokenTtl,
        @DefaultValue("2") int maxRequestsPerWindow,
        @DefaultValue("24h") Duration requestWindow,
        @DefaultValue("20") int ipMaxRequestsPerWindow,
        @DefaultValue("1h") Duration ipWindow) {}
