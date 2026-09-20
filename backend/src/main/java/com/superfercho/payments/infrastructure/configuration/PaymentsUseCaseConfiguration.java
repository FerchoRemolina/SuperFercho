package com.superfercho.payments.infrastructure.configuration;

import com.superfercho.payments.application.port.ClockPort;
import com.superfercho.payments.application.port.PaymentRepository;
import com.superfercho.payments.application.usecase.GetPaymentUseCase;
import com.superfercho.payments.application.usecase.ProcessPaymentUseCase;
import com.superfercho.payments.application.usecase.RefundPaymentUseCase;
import com.superfercho.payments.infrastructure.clock.SystemClockAdapter;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class PaymentsUseCaseConfiguration {

    @Bean
    ClockPort paymentsClockPort(Clock clock) {
        return new SystemClockAdapter(clock);
    }

    @Bean
    ProcessPaymentUseCase processPaymentUseCase(PaymentRepository paymentRepository, ClockPort paymentsClockPort) {
        return new ProcessPaymentUseCase(paymentRepository, paymentsClockPort);
    }

    @Bean
    GetPaymentUseCase getPaymentUseCase(PaymentRepository paymentRepository) {
        return new GetPaymentUseCase(paymentRepository);
    }

    @Bean
    RefundPaymentUseCase refundPaymentUseCase(PaymentRepository paymentRepository, ClockPort paymentsClockPort) {
        return new RefundPaymentUseCase(paymentRepository, paymentsClockPort);
    }
}
