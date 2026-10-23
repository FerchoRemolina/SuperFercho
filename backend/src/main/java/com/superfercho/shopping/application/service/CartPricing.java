package com.superfercho.shopping.application.service;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.ProductCatalogInfo;
import com.superfercho.shopping.application.dto.cart.CartItemResponse;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class CartPricing {

    private CartPricing() {}

    static CartResponse of(Cart cart, ProductCatalogPort catalog) {
        if (cart.items().isEmpty()) {
            return CartResponse.from(cart, List.of(), Money.copZero(), true);
        }
        List<CartItemResponse> items = new ArrayList<>();
        Money subtotal = Money.copZero();
        boolean complete = true;
        for (CartItem item : cart.items()) {
            Optional<ProductCatalogInfo> product = catalog.getProduct(item.productId());
            if (product.isEmpty()) {
                complete = false;
                items.add(CartItemResponse.from(item, null));
                continue;
            }
            Money unitPrice = product.get().currentPrice();
            items.add(CartItemResponse.from(item, unitPrice));
            if (complete) {
                subtotal = subtotal.plus(unitPrice.times(item.quantity()));
            }
        }
        return CartResponse.from(cart, List.copyOf(items), complete ? subtotal : null, complete);
    }
}
