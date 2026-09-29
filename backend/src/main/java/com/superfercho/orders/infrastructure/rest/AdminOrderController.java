package com.superfercho.orders.infrastructure.rest;

import com.superfercho.orders.application.dto.GetAdminSalesPeriodSummaryCommand;
import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.ListAdminOrdersCommand;
import com.superfercho.orders.application.dto.ListAdminRecentBuyersCommand;
import com.superfercho.orders.application.usecase.GetAdminOrderUseCase;
import com.superfercho.orders.application.usecase.GetAdminSalesPeriodSummaryUseCase;
import com.superfercho.orders.application.usecase.ListAdminOrdersUseCase;
import com.superfercho.orders.application.usecase.ListAdminRecentBuyersUseCase;
import com.superfercho.orders.infrastructure.rest.dto.AdminRecentBuyersRestResponse;
import com.superfercho.orders.infrastructure.rest.dto.AdminSalesPeriodSummaryRestResponse;
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
    private final GetAdminSalesPeriodSummaryUseCase getAdminSalesPeriodSummaryUseCase;
    private final ListAdminRecentBuyersUseCase listAdminRecentBuyersUseCase;

    public AdminOrderController(
            ListAdminOrdersUseCase listAdminOrdersUseCase,
            GetAdminOrderUseCase getAdminOrderUseCase,
            GetAdminSalesPeriodSummaryUseCase getAdminSalesPeriodSummaryUseCase,
            ListAdminRecentBuyersUseCase listAdminRecentBuyersUseCase) {
        this.listAdminOrdersUseCase = listAdminOrdersUseCase;
        this.getAdminOrderUseCase = getAdminOrderUseCase;
        this.getAdminSalesPeriodSummaryUseCase = getAdminSalesPeriodSummaryUseCase;
        this.listAdminRecentBuyersUseCase = listAdminRecentBuyersUseCase;
    }

    @GetMapping
    public PagedOrdersRestResponse list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> status) {
        return PagedOrdersRestResponse.from(
                listAdminOrdersUseCase.execute(ListAdminOrdersCommand.of(page, size, status)));
    }

    @GetMapping("/dashboard/sales")
    public AdminSalesPeriodSummaryRestResponse salesPeriodSummary(
            @RequestParam(required = false) String granularity) {
        return AdminSalesPeriodSummaryRestResponse.from(
                getAdminSalesPeriodSummaryUseCase.execute(GetAdminSalesPeriodSummaryCommand.of(granularity)));
    }

    @GetMapping("/dashboard/recent-buyers")
    public AdminRecentBuyersRestResponse recentBuyers(@RequestParam(required = false) Integer limit) {
        return AdminRecentBuyersRestResponse.from(
                listAdminRecentBuyersUseCase.execute(ListAdminRecentBuyersCommand.of(limit)));
    }

    @GetMapping("/{orderId}")
    public OrderRestResponse get(@PathVariable UUID orderId) {
        return OrderRestResponse.from(getAdminOrderUseCase.execute(new GetOrderCommand(orderId)));
    }
}
