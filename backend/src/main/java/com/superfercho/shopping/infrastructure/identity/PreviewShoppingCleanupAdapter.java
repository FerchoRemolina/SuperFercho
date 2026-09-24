package com.superfercho.shopping.infrastructure.identity;

import com.superfercho.identity.application.port.PreviewShoppingCleanupPort;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.FavoriteRepositoryPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class PreviewShoppingCleanupAdapter implements PreviewShoppingCleanupPort {

    private final CartRepositoryPort cartRepository;
    private final FavoriteRepositoryPort favoriteRepository;
    private final ShoppingListRepositoryPort shoppingListRepository;

    public PreviewShoppingCleanupAdapter(
            CartRepositoryPort cartRepository,
            FavoriteRepositoryPort favoriteRepository,
            ShoppingListRepositoryPort shoppingListRepository) {
        this.cartRepository = cartRepository;
        this.favoriteRepository = favoriteRepository;
        this.shoppingListRepository = shoppingListRepository;
    }

    @Override
    public void deleteAllForCustomer(UUID customerId) {
        cartRepository.deleteByCustomerId(customerId);
        favoriteRepository.deleteAllByCustomerId(customerId);
        shoppingListRepository.deleteAllByCustomerId(customerId);
    }
}
