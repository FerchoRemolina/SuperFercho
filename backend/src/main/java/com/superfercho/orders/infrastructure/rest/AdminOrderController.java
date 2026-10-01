package com.superfercho.orders.infrastructure.rest;

import com.superfercho.orders.application.dto.GetAdminDashboardSummaryCommand;
import com.superfercho.orders.application.dto.GetAdminSalesPeriodAnalyticsCommand;
import com.superfercho.orders.application.dto.GetAdminSalesPeriodSummaryCommand;
import com.superfercho.orders.application.dto.GetAdminTopCustomersCommand;
import com.superfercho.orders.application.dto.GetAdminTopProductsCommand;
import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.ListAdminOrdersCommand;
import com.superfercho.orders.application.dto.ListAdminRecentBuyersCommand;
import com.superfercho.orders.application.usecase.GetAdminOrderPeriodSummaryUseCase;
import com.superfercho.orders.application.usecase.GetAdminOrderUseCase;
import com.superfercho.orders.application.usecase.GetAdminSalesPeriodAnalyticsUseCase;
import com.superfercho.orders.application.usecase.GetAdminSalesPeriodSummaryUseCase;
import com.superfercho.orders.application.usecase.GetAdminTopCustomersUseCase;
import com.superfercho.orders.application.usecase.GetAdminTopProductsUseCase;
import com.superfercho.orders.application.usecase.ListAdminOrdersUseCase;
import com.superfercho.orders.application.usecase.ListAdminRecentBuyersUseCase;
import com.superfercho.orders.infrastructure.rest.dto.AdminDashboardSummaryRestResponse;
import com.superfercho.orders.infrastructure.rest.dto.AdminRecentBuyersRestResponse;
import com.superfercho.orders.infrastructure.rest.dto.AdminSalesPeriodSummaryRestResponse;
import com.superfercho.orders.infrastructure.rest.dto.AdminTopCustomersRestResponse;
import com.superfercho.orders.infrastructure.rest.dto.AdminTopProductsRestResponse;
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
    private final GetAdminSalesPeriodAnalyticsUseCase getAdminSalesPeriodAnalyticsUseCase;
    private final GetAdminOrderPeriodSummaryUseCase getAdminOrderPeriodSummaryUseCase;
    private final GetAdminTopProductsUseCase getAdminTopProductsUseCase;
    private final GetAdminTopCustomersUseCase getAdminTopCustomersUseCase;

    public AdminOrderController(
            ListAdminOrdersUseCase listAdminOrdersUseCase,
            GetAdminOrderUseCase getAdminOrderUseCase,
            GetAdminSalesPeriodSummaryUseCase getAdminSalesPeriodSummaryUseCase,
            ListAdminRecentBuyersUseCase listAdminRecentBuyersUseCase,
            GetAdminSalesPeriodAnalyticsUseCase getAdminSalesPeriodAnalyticsUseCase,
            GetAdminOrderPeriodSummaryUseCase getAdminOrderPeriodSummaryUseCase,
            GetAdminTopProductsUseCase getAdminTopProductsUseCase,
            GetAdminTopCustomersUseCase getAdminTopCustomersUseCase) {
        this.listAdminOrdersUseCase = listAdminOrdersUseCase;
        this.getAdminOrderUseCase = getAdminOrderUseCase;
        this.getAdminSalesPeriodSummaryUseCase = getAdminSalesPeriodSummaryUseCase;
        this.listAdminRecentBuyersUseCase = listAdminRecentBuyersUseCase;
        this.getAdminSalesPeriodAnalyticsUseCase = getAdminSalesPeriodAnalyticsUseCase;
        this.getAdminOrderPeriodSummaryUseCase = getAdminOrderPeriodSummaryUseCase;
        this.getAdminTopProductsUseCase = getAdminTopProductsUseCase;
        this.getAdminTopCustomersUseCase = getAdminTopCustomersUseCase;
    }

    @GetMapping
    public PagedOrdersRestResponse list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> status) {
        return PagedOrdersRestResponse.from(
                listAdminOrdersUseCase.execute(ListAdminOrdersCommand.of(page, size, status)));
    }

    /**
     * Sales series. Without from/to keeps the legacy rolling windows
     * (granularity DAY|WEEK|MONTH|YEAR). With from/to queries an arbitrary
     * [from, to) period bucketed by granularity HOUR|DAY|MONTH (default DAY).
     */
    @GetMapping("/dashboard/sales")
    public AdminSalesPeriodSummaryRestResponse salesPeriodSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String granularity) {
        if (from != null || to != null) {
            return AdminSalesPeriodSummaryRestResponse.from(getAdminSalesPeriodAnalyticsUseCase
                    .execute(GetAdminSalesPeriodAnalyticsCommand.of(from, to, granularity)));
        }
        return AdminSalesPeriodSummaryRestResponse.from(
                getAdminSalesPeriodSummaryUseCase.execute(GetAdminSalesPeriodSummaryCommand.of(granularity)));
    }

    /** Order counters for an arbitrary [from, to) period (sales exclude CANCELLED). */
    @GetMapping("/dashboard/summary")
    public AdminDashboardSummaryRestResponse dashboardSummary(
            @RequestParam String from, @RequestParam String to) {
        GetAdminDashboardSummaryCommand command = GetAdminDashboardSummaryCommand.of(from, to);
        return AdminDashboardSummaryRestResponse.from(
                getAdminOrderPeriodSummaryUseCase.execute(command), command.from(), command.to());
    }

    /** Product ranking by units sold for an arbitrary [from, to) period. */
    @GetMapping("/dashboard/products")
    public AdminTopProductsRestResponse topProducts(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort) {
        return AdminTopProductsRestResponse.from(
                getAdminTopProductsUseCase.execute(GetAdminTopProductsCommand.of(from, to, limit, sort)));
    }

    /** Top customers by purchased value for an arbitrary [from, to) period. */
    @GetMapping("/dashboard/customers/top")
    public AdminTopCustomersRestResponse topCustomers(
            @RequestParam String from, @RequestParam String to, @RequestParam(required = false) Integer limit) {
        return AdminTopCustomersRestResponse.from(
                getAdminTopCustomersUseCase.execute(GetAdminTopCustomersCommand.of(from, to, limit)));
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
