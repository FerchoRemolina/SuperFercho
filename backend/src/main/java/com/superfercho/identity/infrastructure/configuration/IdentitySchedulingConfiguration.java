package com.superfercho.identity.infrastructure.configuration;

import com.superfercho.identity.application.usecase.ExpireStorefrontPreviewsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@Profile("!test")
public class IdentitySchedulingConfiguration {

    @Bean
    ExpireStorefrontPreviewsJob expireStorefrontPreviewsJob(
            ExpireStorefrontPreviewsUseCase expireStorefrontPreviewsUseCase) {
        return new ExpireStorefrontPreviewsJob(expireStorefrontPreviewsUseCase);
    }

    public static final class ExpireStorefrontPreviewsJob {

        private static final long ONE_MINUTE_MS = 60_000L;

        private final ExpireStorefrontPreviewsUseCase expireStorefrontPreviewsUseCase;

        ExpireStorefrontPreviewsJob(ExpireStorefrontPreviewsUseCase expireStorefrontPreviewsUseCase) {
            this.expireStorefrontPreviewsUseCase = expireStorefrontPreviewsUseCase;
        }

        @Scheduled(fixedDelay = ONE_MINUTE_MS, initialDelay = ONE_MINUTE_MS)
        public void execute() {
            expireStorefrontPreviewsUseCase.execute();
        }
    }
}
