package com.superfercho.storefront.infrastructure.configuration;

import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.storefront.application.port.AdminPrincipalProvider;
import com.superfercho.storefront.application.port.ClockPort;
import com.superfercho.storefront.application.port.StorefrontContentRepository;
import com.superfercho.storefront.application.port.StorefrontFaqRepository;
import com.superfercho.storefront.application.port.StorefrontMediaPort;
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
import com.superfercho.storefront.infrastructure.identity.AdminPrincipalProviderAdapter;
import com.superfercho.storefront.infrastructure.clock.SystemClockAdapter;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class StorefrontUseCaseConfiguration {

    @Bean
    ClockPort storefrontClockPort(Clock clock) {
        return new SystemClockAdapter(clock);
    }

    @Bean
    AdminPrincipalProvider adminPrincipalProvider(CurrentUserProvider currentUserProvider) {
        return new AdminPrincipalProviderAdapter(currentUserProvider);
    }

    @Bean
    GetStorefrontHeroUseCase getStorefrontHeroUseCase(
            StorefrontContentRepository storefrontContentRepository) {
        return new GetStorefrontHeroUseCase(storefrontContentRepository);
    }

    @Bean
    UpdateStorefrontHeroUseCase updateStorefrontHeroUseCase(
            StorefrontContentRepository storefrontContentRepository,
            AdminPrincipalProvider adminPrincipalProvider,
            ClockPort storefrontClockPort) {
        return new UpdateStorefrontHeroUseCase(
                storefrontContentRepository, adminPrincipalProvider, storefrontClockPort);
    }

    @Bean
    ListActiveStorefrontFaqsUseCase listActiveStorefrontFaqsUseCase(
            StorefrontFaqRepository storefrontFaqRepository) {
        return new ListActiveStorefrontFaqsUseCase(storefrontFaqRepository);
    }

    @Bean
    ListStorefrontFaqsUseCase listStorefrontFaqsUseCase(
            StorefrontFaqRepository storefrontFaqRepository) {
        return new ListStorefrontFaqsUseCase(storefrontFaqRepository);
    }

    @Bean
    CreateStorefrontFaqUseCase createStorefrontFaqUseCase(
            StorefrontFaqRepository storefrontFaqRepository, ClockPort storefrontClockPort) {
        return new CreateStorefrontFaqUseCase(storefrontFaqRepository, storefrontClockPort);
    }

    @Bean
    UpdateStorefrontFaqUseCase updateStorefrontFaqUseCase(
            StorefrontFaqRepository storefrontFaqRepository, ClockPort storefrontClockPort) {
        return new UpdateStorefrontFaqUseCase(storefrontFaqRepository, storefrontClockPort);
    }

    @Bean
    ReorderStorefrontFaqsUseCase reorderStorefrontFaqsUseCase(
            StorefrontFaqRepository storefrontFaqRepository, ClockPort storefrontClockPort) {
        return new ReorderStorefrontFaqsUseCase(storefrontFaqRepository, storefrontClockPort);
    }

    @Bean
    DeleteStorefrontFaqUseCase deleteStorefrontFaqUseCase(
            StorefrontFaqRepository storefrontFaqRepository) {
        return new DeleteStorefrontFaqUseCase(storefrontFaqRepository);
    }

    @Bean
    GetStorefrontFooterSettingsUseCase getStorefrontFooterSettingsUseCase(
            StorefrontContentRepository storefrontContentRepository) {
        return new GetStorefrontFooterSettingsUseCase(storefrontContentRepository);
    }

    @Bean
    UpdateStorefrontFooterSettingsUseCase updateStorefrontFooterSettingsUseCase(
            StorefrontContentRepository storefrontContentRepository,
            AdminPrincipalProvider adminPrincipalProvider,
            ClockPort storefrontClockPort) {
        return new UpdateStorefrontFooterSettingsUseCase(
                storefrontContentRepository, adminPrincipalProvider, storefrontClockPort);
    }

    @Bean
    UpdateStorefrontQrImageUseCase updateStorefrontQrImageUseCase(
            StorefrontContentRepository storefrontContentRepository,
            StorefrontMediaPort storefrontMediaPort,
            AdminPrincipalProvider adminPrincipalProvider,
            ClockPort storefrontClockPort) {
        return new UpdateStorefrontQrImageUseCase(
                storefrontContentRepository,
                storefrontMediaPort,
                adminPrincipalProvider,
                storefrontClockPort);
    }
}
