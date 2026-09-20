package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.cart.ChangeCartItemQuantityCommand;

public interface ChangeCartItemQuantityUseCase {

    CartResponse execute(ChangeCartItemQuantityCommand command);
}
