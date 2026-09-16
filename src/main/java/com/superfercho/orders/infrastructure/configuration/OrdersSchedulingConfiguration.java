package com.superfercho.orders.infrastructure.configuration;

import com.superfercho.orders.application.usecase.AutoConfirmPendingOrdersUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile("!test")
@EnableScheduling
public class OrdersSchedulingConfiguration {

    @Bean
    AutoConfirmPendingOrdersJob autoConfirmPendingOrdersJob(
            AutoConfirmPendingOrdersUseCase autoConfirmPendingOrdersUseCase) {
        return new AutoConfirmPendingOrdersJob(autoConfirmPendingOrdersUseCase);
    }
}
