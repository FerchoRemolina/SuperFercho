package com.superfercho.orders.infrastructure.rest;

import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.ListAdminOrdersCommand;
import com.superfercho.orders.application.usecase.GetAdminOrderUseCase;
import com.superfercho.orders.application.usecase.ListAdminOrdersUseCase;
import com.superfercho.orders.infrastructure.rest.dto.OrderRestResponse;
import com.superfercho.orders.infrastructure.rest.dto.PagedOrdersRestResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final ListAdminOrdersUseCase listAdminOrdersUseCase;
    private final GetAdminOrderUseCase getAdminOrderUseCase;

    public AdminOrderController(
            ListAdminOrdersUseCase listAdminOrdersUseCase, GetAdminOrderUseCase getAdminOrderUseCase) {
        this.listAdminOrdersUseCase = listAdminOrdersUseCase;
        this.getAdminOrderUseCase = getAdminOrderUseCase;
    }

    @GetMapping
    public PagedOrdersRestResponse list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> status) {
        return PagedOrdersRestResponse.from(
                listAdminOrdersUseCase.execute(ListAdminOrdersCommand.of(page, size, status)));
    }

    @GetMapping("/{orderId}")
    public OrderRestResponse get(@PathVariable UUID orderId) {
        return OrderRestResponse.from(getAdminOrderUseCase.execute(new GetOrderCommand(orderId)));
    }
}
