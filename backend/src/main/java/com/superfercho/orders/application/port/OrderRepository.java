package com.superfercho.orders.application.port;

import com.superfercho.orders.application.dto.AdminBusinessPeriodRow;
import com.superfercho.orders.application.dto.AdminCustomerSalesRow;
import com.superfercho.orders.application.dto.AdminOrderFilter;
import com.superfercho.orders.application.dto.AdminProductSalesRow;
import com.superfercho.orders.application.dto.AdminSalesBucketRow;
import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.time.BucketGranularity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    /**
     * Persists a lifecycle transition with an optimistic CAS: the row is only
     * written when its current status still equals {@code fromStatus}. Empty
     * means another writer already changed the status (e.g. cancel vs
     * lifecycle, or two role actions racing the same transition).
     */
    Optional<Order> saveIfCurrent(Order order, OrderStatus fromStatus);

    Optional<Order> findById(UUID orderId);

    PagedResult<Order> findByCustomerId(UUID customerId, PageRequest pageRequest);

    PagedResult<Order> findByCustomerIds(Collection<UUID> customerIds, PageRequest pageRequest);

    /**
     * Orders that already have a payment id, for the given operational customer ids (newest first).
     */
    PagedResult<Order> findOrdersWithPaymentByCustomerIds(
            Collection<UUID> customerIds, PageRequest pageRequest);

    /**
     * Admin orders list page. Applies optional status set, order-number search
     * (case-insensitive contains) and creation {@code [from, to)} range. Always
     * sorted createdAt DESC, id ASC with a correct {@code totalElements}.
     */
    PagedResult<Order> findByAdminFilter(AdminOrderFilter filter, PageRequest pageRequest);

    /** In-progress orders eligible for automatic lifecycle progression. */
    List<Order> findInProgressForLifecycle();

    /**
     * Orders created in {@code [fromInclusive, toExclusive)} whose status is not
     * {@code excludedStatus}. Used by Admin Hub period summaries.
     */
    List<Order> findCreatedBetweenExcludingStatus(
            Instant fromInclusive, Instant toExclusive, OrderStatus excludedStatus);

    void deleteAllByCustomerId(UUID customerId);

    /**
     * Sales totals (CANCELLED excluded) grouped by {@code granularity} buckets over
     * {@code [fromInclusive, toExclusive)}. Bucket boundaries follow America/Bogota.
     */
    List<AdminSalesBucketRow> aggregateSalesBuckets(
            BucketGranularity granularity, Instant fromInclusive, Instant toExclusive);

    /**
     * Business counters for {@code [fromInclusive, toExclusive)}: delivered by
     * {@code delivered_at}, cancelled by {@code cancelled_at}, sales = DELIVERED
     * totals by {@code delivered_at}, and in-process as the CURRENT live count
     * (no period). Aggregated in the database.
     */
    AdminBusinessPeriodRow summarizeBusinessPeriod(Instant fromInclusive, Instant toExclusive);

    /** DELIVERED orders whose {@code delivered_at} falls in {@code [fromInclusive, toExclusive)}. */
    List<Order> findDeliveredBetween(Instant fromInclusive, Instant toExclusive);

    /**
     * Units sold per product (CANCELLED excluded) over {@code [fromInclusive, toExclusive)},
     * ordered by quantity (ascending when {@code ascending}). Limited to {@code limit} rows.
     */
    List<AdminProductSalesRow> findTopProductsByQuantity(
            Instant fromInclusive, Instant toExclusive, int limit, boolean ascending);

    /**
     * Purchased value per customer (CANCELLED excluded) over
     * {@code [fromInclusive, toExclusive)}, ordered by total desc. Limited to {@code limit} rows.
     */
    List<AdminCustomerSalesRow> findTopCustomersByOrders(java.time.Instant fromInclusive, java.time.Instant toExclusive, int limit);

    List<AdminCustomerSalesRow> findTopCustomersByTotal(
            Instant fromInclusive, Instant toExclusive, int limit);
}
