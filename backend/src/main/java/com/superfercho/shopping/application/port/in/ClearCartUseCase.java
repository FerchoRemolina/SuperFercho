package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.cart.ClearCartCommand;

public interface ClearCartUseCase {

    CartResponse execute(ClearCartCommand command);
}
