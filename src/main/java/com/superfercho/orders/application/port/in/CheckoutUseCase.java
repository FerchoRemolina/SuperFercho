package com.superfercho.orders.application.port.in;

import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutResult;

public interface CheckoutUseCase {

    CheckoutResult execute(CheckoutCommand command);
}
