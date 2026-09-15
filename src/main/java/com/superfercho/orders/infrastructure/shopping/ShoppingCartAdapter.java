package com.superfercho.orders.infrastructure.shopping;

import com.superfercho.orders.application.dto.CartItemSnapshot;
import com.superfercho.orders.application.dto.CartSnapshot;
import com.superfercho.orders.application.port.ShoppingCartPort;
import com.superfercho.shopping.application.dto.cart.CartItemResponse;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.cart.ClearCartCommand;
import com.superfercho.shopping.application.dto.cart.GetCartQuery;
import com.superfercho.shopping.application.port.in.ClearCartUseCase;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ShoppingCartAdapter implements ShoppingCartPort {

    private final GetCartUseCase getCartUseCase;
    private final ClearCartUseCase clearCartUseCase;

    public ShoppingCartAdapter(GetCartUseCase getCartUseCase, ClearCartUseCase clearCartUseCase) {
        this.getCartUseCase = getCartUseCase;
        this.clearCartUseCase = clearCartUseCase;
    }

    @Override
    public CartSnapshot getActiveCart(UUID customerId) {
        return toSnapshot(getCartUseCase.execute(new GetCartQuery(customerId)));
    }

    @Override
    public void clearCart(UUID customerId) {
        clearCartUseCase.execute(new ClearCartCommand(customerId));
    }

    private static CartSnapshot toSnapshot(CartResponse cart) {
        return new CartSnapshot(
                cart.id(), cart.items().stream().map(ShoppingCartAdapter::toItem).toList());
    }

    private static CartItemSnapshot toItem(CartItemResponse item) {
        return new CartItemSnapshot(item.productId(), item.quantity(), item.priceAtAddition());
    }
}
