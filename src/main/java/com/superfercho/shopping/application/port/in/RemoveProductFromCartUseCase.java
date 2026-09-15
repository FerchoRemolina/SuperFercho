package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.cart.RemoveProductFromCartCommand;

public interface RemoveProductFromCartUseCase {

    CartResponse execute(RemoveProductFromCartCommand command);
}
