package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.cart.AddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.CartResponse;

public interface AddProductToCartUseCase {

    CartResponse execute(AddProductToCartCommand command);
}
