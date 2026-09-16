package com.superfercho.payments.infrastructure.rest;

import com.superfercho.payments.application.dto.GetPaymentCommand;
import com.superfercho.payments.application.usecase.GetPaymentUseCase;
import com.superfercho.payments.infrastructure.rest.dto.PaymentRestResponse;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final GetPaymentUseCase getPaymentUseCase;

    public PaymentController(GetPaymentUseCase getPaymentUseCase) {
        this.getPaymentUseCase = getPaymentUseCase;
    }

    @GetMapping("/{paymentId}")
    public PaymentRestResponse get(@PathVariable UUID paymentId) {
        return PaymentRestResponse.from(getPaymentUseCase.execute(new GetPaymentCommand(paymentId)));
    }
}
