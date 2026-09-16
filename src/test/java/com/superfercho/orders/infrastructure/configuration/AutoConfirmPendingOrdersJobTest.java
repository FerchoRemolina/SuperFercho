package com.superfercho.orders.infrastructure.configuration;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.superfercho.orders.application.usecase.AutoConfirmPendingOrdersUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AutoConfirmPendingOrdersJobTest {

    @Mock
    private AutoConfirmPendingOrdersUseCase autoConfirmPendingOrdersUseCase;

    private AutoConfirmPendingOrdersJob job;

    @BeforeEach
    void setUp() {
        job = new AutoConfirmPendingOrdersJob(autoConfirmPendingOrdersUseCase);
    }

    @Test
    void shouldInvokeAutoConfirmUseCaseOncePerExecution() {
        job.execute();

        verify(autoConfirmPendingOrdersUseCase, times(1)).execute();
        verifyNoMoreInteractions(autoConfirmPendingOrdersUseCase);
    }
}
