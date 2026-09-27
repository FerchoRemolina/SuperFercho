package com.superfercho.platform.demo.historical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.identity.application.validation.CustomerRegistrationRules;
import com.superfercho.platform.demo.historical.HistoricalDemoBlueprint.CustomerSpec;
import com.superfercho.platform.demo.historical.HistoricalDemoBlueprint.OrderOutcome;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HistoricalDemoBlueprintTest {

    @Test
    void shouldDefineExactlyTenCustomersAndThirtyTwoOrders() {
        List<CustomerSpec> customers = HistoricalDemoBlueprint.customers();
        assertEquals(HistoricalDemoBlueprint.EXPECTED_CUSTOMERS, customers.size());
        int orders = customers.stream().mapToInt(customer -> customer.orders().size()).sum();
        assertEquals(HistoricalDemoBlueprint.EXPECTED_ORDERS, orders);
    }

    @Test
    void shouldOnlyUseDeliveredAndCancelledHistoricalOrders() {
        int delivered = 0;
        int cancelled = 0;
        for (CustomerSpec customer : HistoricalDemoBlueprint.customers()) {
            assertEquals(1, customer.addresses().stream().filter(HistoricalDemoBlueprint.AddressSpec::isDefault).count());
            for (var order : customer.orders()) {
                if (order.outcome() == OrderOutcome.DELIVERED) {
                    delivered++;
                } else {
                    cancelled++;
                }
                assertFalse(order.productCodes().isEmpty());
            }
        }
        assertEquals(HistoricalDemoBlueprint.EXPECTED_ORDERS, delivered + cancelled);
        assertTrue(delivered > 0);
        assertTrue(cancelled > 0);
    }

    @Test
    void shouldUseUniqueApprovedEmailsAndDocuments() {
        Set<String> emails = new HashSet<>();
        Set<String> documents = new HashSet<>();
        for (CustomerSpec customer : HistoricalDemoBlueprint.customers()) {
            assertTrue(emails.add(customer.email()));
            assertTrue(documents.add(customer.documentNumber()));
            assertEquals(10, customer.documentNumber().length());
            assertTrue(customer.phone().matches("^3\\d{9}$"));
        }
        assertEquals(10, emails.size());
        assertEquals(10, documents.size());
    }

    @Test
    void shouldUseDemoPasswordWithinPublicRegistrationRules() {
        assertEquals("SuperF123!", HistoricalDemoBlueprint.PASSWORD);
        CustomerRegistrationRules.requirePassword(HistoricalDemoBlueprint.PASSWORD);
    }

    @Test
    void shouldMatchApprovedPerCustomerOrderCounts() {
        List<CustomerSpec> customers = HistoricalDemoBlueprint.customers();
        assertEquals(5, customers.get(0).orders().size());
        assertEquals(4, customers.get(1).orders().size());
        assertEquals(4, customers.get(2).orders().size());
        assertEquals(3, customers.get(3).orders().size());
        assertEquals(3, customers.get(4).orders().size());
        assertEquals(1, customers.get(5).orders().size());
        assertEquals(3, customers.get(6).orders().size());
        assertEquals(5, customers.get(7).orders().size());
        assertEquals(1, customers.get(8).orders().size());
        assertEquals(3, customers.get(9).orders().size());
    }
}
