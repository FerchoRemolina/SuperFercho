package com.superfercho.identity.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "superfercho.email")
public record EmailProperties(
        @DefaultValue("console") String provider,
        @DefaultValue("http://localhost:3000") String recoveryBaseUrl) {}
