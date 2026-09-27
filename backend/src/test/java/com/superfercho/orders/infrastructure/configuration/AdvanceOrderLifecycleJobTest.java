package com.superfercho.orders.infrastructure.configuration;

import static org.mockito.Mockito.verify;

import com.superfercho.orders.application.usecase.AdvanceOrderLifecycleUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AdvanceOrderLifecycleJobTest {

    @Test
    void shouldDelegateToAdvanceOrderLifecycleUseCase() {
        AdvanceOrderLifecycleUseCase useCase = Mockito.mock(AdvanceOrderLifecycleUseCase.class);

        new AdvanceOrderLifecycleJob(useCase).execute();

        verify(useCase).execute();
    }
}
