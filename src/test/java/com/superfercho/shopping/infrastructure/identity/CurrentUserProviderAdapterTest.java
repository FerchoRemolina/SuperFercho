package com.superfercho.shopping.infrastructure.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrentUserProviderAdapterTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private com.superfercho.identity.application.port.CurrentUserProvider identityCurrentUserProvider;

    private CurrentUserProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CurrentUserProviderAdapter(identityCurrentUserProvider);
    }

    @Test
    void shouldDelegateToIdentityCurrentUserProvider() {
        when(identityCurrentUserProvider.getCurrentUserId()).thenReturn(USER_ID);

        assertEquals(USER_ID, adapter.getCurrentUserId());
        verify(identityCurrentUserProvider).getCurrentUserId();
    }

    @Test
    void shouldPropagateUnauthenticatedUserException() {
        UnauthenticatedUserException unauthenticated = new UnauthenticatedUserException();
        when(identityCurrentUserProvider.getCurrentUserId()).thenThrow(unauthenticated);

        UnauthenticatedUserException thrown =
                assertThrows(UnauthenticatedUserException.class, adapter::getCurrentUserId);
        assertSame(unauthenticated, thrown);
        verify(identityCurrentUserProvider).getCurrentUserId();
    }
}
