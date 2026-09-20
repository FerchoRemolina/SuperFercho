package com.superfercho.payments.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.payments.application.dto.GetPaymentCommand;
import com.superfercho.payments.application.dto.PaymentResponse;
import com.superfercho.payments.application.exception.PaymentNotFoundException;
import com.superfercho.payments.application.usecase.GetPaymentUseCase;
import com.superfercho.payments.domain.model.PaymentMethod;
import com.superfercho.payments.domain.model.PaymentStatus;
import com.superfercho.platform.error.ApiExceptionHandler;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PaymentsExceptionHandler.class, ApiExceptionHandler.class})
class PaymentControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-05-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-05-01T10:05:00Z");
    private static final UUID PAYMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Money AMOUNT = Money.cop(new BigDecimal("21.00"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPaymentUseCase getPaymentUseCase;

    @Test
    void shouldGetPaymentById() throws Exception {
        when(getPaymentUseCase.execute(new GetPaymentCommand(PAYMENT_ID))).thenReturn(paymentResponse());

        mockMvc.perform(get("/api/v1/payments/{paymentId}", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.amount.amount").value(21.00))
                .andExpect(jsonPath("$.amount.currency").value("COP"))
                .andExpect(jsonPath("$.paymentMethod").value("SIMULATED_CARD"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.providerReference").value("sim-approved"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()))
                .andExpect(jsonPath("$.refundedAt").value(UPDATED_AT.toString()));

        verify(getPaymentUseCase).execute(new GetPaymentCommand(PAYMENT_ID));
    }

    @Test
    void shouldMapPaymentNotFoundToNotFound() throws Exception {
        when(getPaymentUseCase.execute(any())).thenThrow(new PaymentNotFoundException(PAYMENT_ID));

        mockMvc.perform(get("/api/v1/payments/{paymentId}", PAYMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Payment not found: " + PAYMENT_ID))
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
    }

    private static PaymentResponse paymentResponse() {
        return new PaymentResponse(
                PAYMENT_ID,
                ORDER_ID,
                AMOUNT,
                PaymentMethod.SIMULATED_CARD,
                PaymentStatus.APPROVED,
                "sim-approved",
                CREATED_AT,
                UPDATED_AT,
                UPDATED_AT);
    }
}
