package com.superfercho.shopping.infrastructure.identity;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.superfercho.shopping.application.port.out.CartAddOperationPort;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.FavoriteRepositoryPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreviewShoppingCleanupAdapterTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private CartRepositoryPort cartRepository;

    @Mock
    private FavoriteRepositoryPort favoriteRepository;

    @Mock
    private ShoppingListRepositoryPort shoppingListRepository;

    @Mock
    private CartAddOperationPort cartAddOperationPort;

    @InjectMocks
    private PreviewShoppingCleanupAdapter adapter;

    @Test
    void deleteAllForCustomerRemovesReceiptsAndShoppingData() {
        adapter.deleteAllForCustomer(CUSTOMER_ID);

        verify(cartAddOperationPort).deleteAllByCustomerId(CUSTOMER_ID);
        verify(cartRepository).deleteByCustomerId(CUSTOMER_ID);
        verify(favoriteRepository).deleteAllByCustomerId(CUSTOMER_ID);
        verify(shoppingListRepository).deleteAllByCustomerId(CUSTOMER_ID);
        verifyNoMoreInteractions(
                cartAddOperationPort, cartRepository, favoriteRepository, shoppingListRepository);
    }
}
