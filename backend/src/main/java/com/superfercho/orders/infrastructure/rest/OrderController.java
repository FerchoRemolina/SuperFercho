package com.superfercho.orders.infrastructure.rest;

import com.superfercho.orders.application.dto.CancelOrderCommand;
import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutItem;
import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.ListOrdersCommand;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.application.usecase.ListOrdersUseCase;
import com.superfercho.orders.infrastructure.configuration.TransactionalCancelOrderUseCase;
import com.superfercho.orders.infrastructure.configuration.TransactionalCheckoutUseCase;
import com.superfercho.orders.infrastructure.rest.dto.CheckoutRequest;
import com.superfercho.orders.infrastructure.rest.dto.CheckoutRestResponse;
import com.superfercho.orders.infrastructure.rest.dto.OrderRestResponse;
import com.superfercho.orders.infrastructure.rest.dto.PagedOrdersRestResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final TransactionalCheckoutUseCase transactionalCheckoutUseCase;
    private final TransactionalCancelOrderUseCase transactionalCancelOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ListOrdersUseCase listOrdersUseCase;

    public OrderController(
            TransactionalCheckoutUseCase transactionalCheckoutUseCase,
            TransactionalCancelOrderUseCase transactionalCancelOrderUseCase,
            GetOrderUseCase getOrderUseCase,
            ListOrdersUseCase listOrdersUseCase) {
        this.transactionalCheckoutUseCase = transactionalCheckoutUseCase;
        this.transactionalCancelOrderUseCase = transactionalCancelOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.listOrdersUseCase = listOrdersUseCase;
    }

    @PostMapping
    public ResponseEntity<CheckoutRestResponse> checkout(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CheckoutRequest request) {
        CheckoutRestResponse body = CheckoutRestResponse.from(
                transactionalCheckoutUseCase.execute(toCheckoutCommand(request, idempotencyKey)));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{orderId}")
                        .buildAndExpand(body.orderId())
                        .toUri())
                .body(body);
    }

    @GetMapping
    public PagedOrdersRestResponse list(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return PagedOrdersRestResponse.from(listOrdersUseCase.execute(new ListOrdersCommand(page, size)));
    }

    @GetMapping("/{orderId}")
    public OrderRestResponse get(@PathVariable UUID orderId) {
        return OrderRestResponse.from(getOrderUseCase.execute(new GetOrderCommand(orderId)));
    }

    @PostMapping("/{orderId}/cancel")
    public OrderRestResponse cancel(@PathVariable UUID orderId) {
        return OrderRestResponse.from(transactionalCancelOrderUseCase.execute(new CancelOrderCommand(orderId)));
    }

    private static CheckoutCommand toCheckoutCommand(CheckoutRequest request, String idempotencyKey) {
        List<CheckoutItem> items = request.items() == null
                ? List.of()
                : request.items().stream()
                        .map(item -> new CheckoutItem(item.productId(), item.quantity(), item.expectedUnitPrice()))
                        .toList();
        return new CheckoutCommand(request.addressId(), request.paymentMethod(), items, idempotencyKey);
    }
}
