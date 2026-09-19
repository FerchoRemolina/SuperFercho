package com.superfercho.orders.application.port.in;

import com.superfercho.orders.application.dto.CancelOrderCommand;
import com.superfercho.orders.application.dto.OrderResult;

public interface CancelOrderUseCase {

    OrderResult execute(CancelOrderCommand command);
}
