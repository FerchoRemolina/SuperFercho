package com.superfercho.orders.infrastructure.configuration;

import com.superfercho.orders.application.usecase.AdvanceOrderLifecycleUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile("!test")
@EnableScheduling
public class OrdersSchedulingConfiguration {

    @Bean
    AdvanceOrderLifecycleJob advanceOrderLifecycleJob(
            AdvanceOrderLifecycleUseCase advanceOrderLifecycleUseCase) {
        return new AdvanceOrderLifecycleJob(advanceOrderLifecycleUseCase);
    }
}
