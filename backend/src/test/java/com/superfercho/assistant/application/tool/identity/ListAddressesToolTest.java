package com.superfercho.assistant.application.tool.identity;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.assistant.application.exception.InvalidToolArgumentsException;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolRegistry;
import com.superfercho.identity.application.usecase.ListAddressesUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListAddressesToolTest {

    @Mock
    private ListAddressesUseCase listAddressesUseCase;

    @Test
    void delegatesToListAddressesUseCaseWithoutModelIdentity() {
        when(listAddressesUseCase.execute()).thenReturn(List.of());

        assertTrue(new ListAddressesTool(listAddressesUseCase).execute(ToolArguments.of(Map.of())).success());
        verify(listAddressesUseCase).execute();
    }

    @Test
    void rejectsUserIdProvidedByTheModel() {
        ToolRegistry registry = new ToolRegistry(List.of(new ListAddressesTool(listAddressesUseCase)));

        assertThrows(
                InvalidToolArgumentsException.class,
                () -> registry.execute("list_addresses", Map.of("userId", "11111111-1111-1111-1111-111111111111")));
    }
}
