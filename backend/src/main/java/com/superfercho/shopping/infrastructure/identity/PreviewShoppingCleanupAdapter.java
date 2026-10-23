package com.superfercho.shopping.infrastructure.identity;

import com.superfercho.identity.application.port.CustomerAccountShoppingCleanupPort;
import com.superfercho.identity.application.port.PreviewShoppingCleanupPort;
import com.superfercho.shopping.application.port.out.CartAddOperationPort;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.FavoriteRepositoryPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class PreviewShoppingCleanupAdapter
        implements PreviewShoppingCleanupPort, CustomerAccountShoppingCleanupPort {

    private final CartRepositoryPort cartRepository;
    private final FavoriteRepositoryPort favoriteRepository;
    private final ShoppingListRepositoryPort shoppingListRepository;
    private final CartAddOperationPort cartAddOperationPort;

    public PreviewShoppingCleanupAdapter(
            CartRepositoryPort cartRepository,
            FavoriteRepositoryPort favoriteRepository,
            ShoppingListRepositoryPort shoppingListRepository,
            CartAddOperationPort cartAddOperationPort) {
        this.cartRepository = cartRepository;
        this.favoriteRepository = favoriteRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.cartAddOperationPort = cartAddOperationPort;
    }

    @Override
    public void deleteAllForCustomer(UUID customerId) {
        cartAddOperationPort.deleteAllByCustomerId(customerId);
        cartRepository.deleteByCustomerId(customerId);
        favoriteRepository.deleteAllByCustomerId(customerId);
        shoppingListRepository.deleteAllByCustomerId(customerId);
    }
}
