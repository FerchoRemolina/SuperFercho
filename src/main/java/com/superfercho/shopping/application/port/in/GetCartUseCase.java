package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.cart.GetCartQuery;

public interface GetCartUseCase {

    CartResponse execute(GetCartQuery query);
}
